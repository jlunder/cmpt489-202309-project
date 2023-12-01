package synth.algorithms.lia;

import java.util.*;

public class LinearSolution implements Comparable<LinearSolution> {
    private final Map<Term, Integer> coefficientMap;

    // A packed map of the terms and their values; negative values are (negated)
    // term indexes, positive values are coefficients. Sequential coefficients
    // correspond to sequential terms.
    private final int[] signature;

    public Map<Term, Integer> coefficients() {
        return coefficientMap;
    }

    public LinearSolution(Map<Term, Integer> coefficientMap) {
        this.coefficientMap = coefficientMap;

        var accum = new ArrayList<Integer>(coefficientMap.size() * 2);
        var terms = new ArrayList<Term>(coefficientMap.keySet());
        Collections.sort(terms);
        int expected = 0;
        for (var t : terms) {
            var i = t.index();
            // Only write the term index if it's not sequential
            if (i != expected) {
                accum.add(-i);
                expected = i;
            }
            accum.add(coefficientMap.get(t));
            ++expected;
        }
        signature = new int[accum.size()];
        for (int i = 0; i < accum.size(); ++i) {
            signature[i] = accum.get(i);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this) || (obj instanceof LinearSolution)
                && (Arrays.equals(signature, ((LinearSolution) obj).signature));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(signature);
    }

    @Override
    public int compareTo(LinearSolution other) {
        if (other == null) {
            return 1;
        }
        if (other == this) {
            return 0;
        }
        return Arrays.compare(signature, other.signature);
    }
}
