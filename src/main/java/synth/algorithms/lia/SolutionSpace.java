package synth.algorithms.lia;

import java.util.Arrays;
import java.util.List;

public class SolutionSpace {
    public static SolutionSpace EMPTY = new SolutionSpace();

    String[] terms;
    List<Integer>[] solutions;

    public int numTerms() {
        return terms.length;
    }

    public String getTerm(int i) {
        return terms[i];
    }

    public int numSolutions() {
        return solutions.length;
    }

    public List<Integer> getSolution(int i) {
        return solutions[i];
    }

    private SolutionSpace() {
        terms = new String[0];
        @SuppressWarnings("unchecked")
        var sols = (List<Integer>[]) new List[0];
        this.solutions = sols;
    }

    public SolutionSpace(String[] terms, int[][] solutions) {
        this.terms = terms;
        @SuppressWarnings("unchecked")
        var sols = (List<Integer>[]) new List[solutions.length];
        this.solutions = sols;
        for (int i = 0; i < solutions.length; ++i) {
            assert solutions[i].length == terms.length;
            Integer[] sol = new Integer[solutions[i].length];
            for (int j = 0; j < solutions[i].length; ++j) {
                sol[j] = solutions[i][j];
            }
            this.solutions[i] = Arrays.asList(sol);
        }
        this.solutions = sols;
    }

    SolutionSpace intersect(SolutionSpace other) {
        return null;
    }
}
