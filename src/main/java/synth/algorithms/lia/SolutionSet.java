package synth.algorithms.lia;

import java.util.*;

public final class SolutionSet {
    public static SolutionSet EMPTY = new SolutionSet();

    private final Set<LinearSolution> solutions;

    public boolean isEmpty() {
        return solutions.isEmpty();
    }

    public Set<LinearSolution> solutions() {
        return solutions;
    }

    private SolutionSet() {
        solutions = Set.of();
    }

    public SolutionSet(Collection<LinearSolution> solutions) {
        this.solutions = new HashSet<LinearSolution>(solutions);
    }

    SolutionSet intersect(SolutionSet other) {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SolutionSet)) {
            return false;
        }
        SolutionSet otherSols = (SolutionSet) obj;
        return solutions.equals(otherSols.solutions);
    }

    @Override
    public int hashCode() {
        return solutions.hashCode() * 29;
    }
}
