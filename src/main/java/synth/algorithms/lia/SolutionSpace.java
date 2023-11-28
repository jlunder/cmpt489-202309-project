package synth.algorithms.lia;

public class SolutionSpace {
    public class Term {
    }

    public static SolutionSpace EMPTY = new SolutionSpace();

    Term[] terms;
    int[][] solutions;

    SolutionSpace() {

    }

    SolutionSpace intersect(SolutionSpace other) {
        return null;
    }
}
