package synth.algorithms.lia;

import java.util.*;

public class SolutionSet {
    public static SolutionSet EMPTY = new SolutionSet();

    Term[] terms;
    List<Integer>[] coefficients;

    public int numTerms() {
        return terms.length;
    }

    public Term getTerm(int i) {
        return terms[i];
    }

    public int numSolutions() {
        return coefficients.length;
    }

    public List<Integer> getCoefficients(int i) {
        return coefficients[i];
    }

    public Map<Term, Integer> getSolution(int i) {
        HashMap<Term, Integer> solution = new HashMap<>();
        var coeffs = coefficients[i];
        for(int j = 0; j < terms.length; ++j) {
            Integer c = coeffs.get(j);
            if(c != 0) {
                solution.put(terms[j], c);
            }
        }
        return solution;
    }

    private SolutionSet() {
        terms = new Term[0];
        @SuppressWarnings("unchecked")
        var sols = (List<Integer>[]) new List[0];
        this.coefficients = sols;
    }

    public SolutionSet(Term[] terms, int[][] solutions) {
        this.terms = terms;
        @SuppressWarnings("unchecked")
        var sols = (List<Integer>[]) new List[solutions.length];
        this.coefficients = sols;
        for (int i = 0; i < solutions.length; ++i) {
            assert solutions[i].length == terms.length;
            Integer[] sol = new Integer[solutions[i].length];
            for (int j = 0; j < solutions[i].length; ++j) {
                sol[j] = solutions[i][j];
            }
            this.coefficients[i] = Arrays.asList(sol);
        }
        this.coefficients = sols;
    }

    SolutionSet intersect(SolutionSet other) {
        return null;
    }
}
