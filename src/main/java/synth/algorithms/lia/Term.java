package synth.algorithms.lia;

import java.util.HashMap;

import synth.core.Environment;

public final class Term implements Comparable<Term> {
    public static final int MAX_INDEX_POWER = 1024;
    public static final int MAX_INDEX = MAX_INDEX_POWER * MAX_INDEX_POWER * MAX_INDEX_POWER;

    // private static final Term[] arrayCache = new Term[4096];
    private static final HashMap<Integer, Term> hashCache = new HashMap<>();

    public static Term fromIndex(int index) {
        assert index >= 0 && index < MAX_INDEX;
        // if (index < arrayCache.length) {
        // Term term = arrayCache[index];
        // if (term == null) {
        // int xPower = contractBitsBy3(index >> 0);
        // int yPower = contractBitsBy3(index >> 1);
        // int zPower = contractBitsBy3(index >> 2);
        // term = new Term(xPower, yPower, zPower, index);
        // arrayCache[index] = term;
        // }
        // return term;
        // } else {
        return hashCache.computeIfAbsent(index, cacheIndex -> {
            int xPower = contractBitsBy3(index >> 0);
            int yPower = contractBitsBy3(index >> 1);
            int zPower = contractBitsBy3(index >> 2);
            return new Term(xPower, yPower, zPower, cacheIndex);
        });
        // }
    }

    public static int computeIndex(int xPower, int yPower, int zPower) {
        return expandBitsBy3(xPower << 0)
                | (expandBitsBy3(yPower) << 1)
                | (expandBitsBy3(zPower) << 2);
    }

    private static int expandBitsBy3(int i) {
        if (i < 0 || i >= 1024) {
            return -1;
        }
        return ((i & (1 << 9)) << (27 - 9)) | ((i & (1 << 8)) << (24 - 8))
                | ((i & (1 << 7)) << (21 - 7)) | ((i & (1 << 6)) << (18 - 6))
                | ((i & (1 << 5)) << (15 - 5)) | ((i & (1 << 4)) << (12 - 4))
                | ((i & (1 << 3)) << (9 - 3)) | ((i & (1 << 2)) << (6 - 2))
                | ((i & (1 << 1)) << (3 - 1)) | ((i & (1 << 0)) << (0 - 0));
    }

    private static int contractBitsBy3(int i) {
        if (i < 0 || i >= (1 << 30)) {
            return -1;
        }
        return (i >> (27 - 9)) & (1 << 9) | (i >> (24 - 8)) & (1 << 8)
                | (i >> (21 - 7)) & (1 << 7) | (i >> (18 - 6)) & (1 << 6)
                | (i >> (15 - 5)) & (1 << 5) | (i >> (12 - 4)) & (1 << 4)
                | (i >> (9 - 3)) & (1 << 3) | (i >> (6 - 2)) & (1 << 2)
                | (i >> (3 - 1)) & (1 << 1) | (i >> (0 - 0)) & (1 << 0);
    }

    private final String name;
    private final String asString;
    private final int xPower;
    private final int yPower;
    private final int zPower;
    private final int index;

    public static Term make(int xPower, int yPower, int zPower) {
        assert xPower < MAX_INDEX_POWER && yPower < MAX_INDEX_POWER && zPower < MAX_INDEX_POWER;
        return fromIndex(computeIndex(xPower, yPower, zPower));
    }

    private Term(int xPower, int yPower, int zPower, int index) {
        assert xPower >= 0 && yPower >= 0 && zPower >= 0;
        if (xPower == 0 && yPower == 0 && zPower == 0) {
            this.name = "c";
            this.asString = "1";
        } else {
            this.name = "a_" + (xPower > 0 ? "x" + (xPower > 1 ? xPower : "") : "")
                    + (yPower > 0 ? "y" + (yPower > 1 ? yPower : "") : "")
                    + (zPower > 0 ? "z" + (zPower > 1 ? zPower : "") : "");
            this.asString = (xPower > 0 ? "x" + (xPower > 1 ? "^" + xPower : "") : "")
                    + (xPower > 0 && yPower > 0 ? " " : "")
                    + (yPower > 0 ? "y" + (yPower > 1 ? "^" + yPower : "") : "")
                    + ((xPower > 0 || yPower > 0) && (zPower > 0) ? " " : "")
                    + (zPower > 0 ? "z" + (zPower > 1 ? "^" + zPower : "") : "");
        }
        this.xPower = xPower;
        this.yPower = yPower;
        this.zPower = zPower;
        // Z-order/Morton code/Lebesgue curve/swizzle the powers to make an index
        assert index == computeIndex(xPower, yPower, zPower);
        this.index = index;
    }

    public String name() {
        return this.name;
    }

    public int index() {
        return this.index;
    }

    public int evalTerm(Environment env) {
        int result = 1;
        int x = env.x();
        for (int i = 0; i < xPower; ++i)
            result *= x;
        int y = env.y();
        for (int i = 0; i < yPower; ++i)
            result *= y;
        int z = env.z();
        for (int i = 0; i < zPower; ++i)
            result *= z;
        return result;
    }

    @Override
    public int hashCode() {
        if (index >= 0) {
            return index;
        } else {
            return ((1077021629 + xPower) * 1077036929 + yPower) * 1177038887 + zPower;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Term)) {
            return false;
        }
        Term otherTerm = (Term) other;
        if (index >= 0 && otherTerm.index != index) {
            return false;
        }
        return (otherTerm.xPower == xPower) && (otherTerm.yPower == yPower) && (otherTerm.zPower == zPower);
    }

    @Override
    public int compareTo(Term other) {
        if (other == null) {
            return 1;
        }
        if (other == this) {
            return 0;
        }
        if ((index & other.index) != -1) {
            // either is not -1; unsigned compare will compare -1 larger than any positive
            // index
            return Integer.compareUnsigned(index, other.index);
        }
        int order = Math.max(Math.max(xPower, yPower), zPower);
        int otherOrder = Math.max(Math.max(other.xPower, other.yPower), other.zPower);
        if (order != otherOrder) {
            return Integer.compare(order, otherOrder);
        }
        for (int i = 0; i < 3; ++i) {
            int shift = (3 - i) * 10;
            int selfIndex = computeIndex(xPower >> shift, yPower >> shift, zPower >> shift);
            int otherIndex = computeIndex(other.xPower >> shift, other.yPower >> shift, other.zPower >> shift);
            if (selfIndex != otherIndex) {
                return Integer.compare(selfIndex, otherIndex);
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return asString;
    }

}
