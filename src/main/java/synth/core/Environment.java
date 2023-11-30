package synth.core;

public final class Environment implements Comparable<Environment> {
    private final int x;
    private final int y;
    private final int z;

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public Environment(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "x = " + x + ", y = " + y + ", z = " + z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Environment)) {
            return false;
        }
        Environment envObj = (Environment) obj;
        return x == envObj.x && y == envObj.y && z == envObj.z;
    }

    @Override
    public int hashCode() {
        return (((1077022421 + x) * 1077028637 + y) * 1077026147 + z) * 1077026141;
    }

    @Override
    public int compareTo(Environment env) {
        int res = Integer.compare(z, env.z);
        if (res != 0) {
            return res;
        }
        res = Integer.compare(y, env.y);
        if (res != 0) {
            return res;
        }
        return Integer.compare(x, env.x);
    }

}
