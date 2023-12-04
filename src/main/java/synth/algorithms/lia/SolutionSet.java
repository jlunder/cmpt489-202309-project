package synth.algorithms.lia;

import java.util.*;

public final class SolutionSet {
    public static SolutionSet EMPTY = new SolutionSet();

    private final LinearSolution representativeSolution;
    private final Set<LinearSolution> solutions;

    public boolean isEmpty() {
        return representativeSolution == null;
    }

    public LinearSolution representativeSolution() {
        return representativeSolution;
    }

    public Set<LinearSolution> solutions() {
        return solutions;
    }

    private SolutionSet() {
        representativeSolution = null;
        solutions = Set.of();
    }

    public SolutionSet(Collection<LinearSolution> solutions) {
        this.representativeSolution = solutions.iterator().next();
        this.solutions = new HashSet<LinearSolution>(solutions);
    }

    private SolutionSet(LinearSolution representativeSolution, Set<LinearSolution> solutions) {
        assert solutions.contains(representativeSolution);
        this.representativeSolution = representativeSolution;
        this.solutions = solutions;
    }

    SolutionSet intersect(SolutionSet other) {
        var intersectedSols = new HashSet<LinearSolution>(solutions);
        if (intersectedSols.isEmpty()) {
            return EMPTY;
        }
        intersectedSols.retainAll(other.solutions());
        var rep = representativeSolution;
        if (!intersectedSols.contains(rep)) {
            rep = intersectedSols.iterator().next();
        }
        return new SolutionSet(rep, intersectedSols);
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
