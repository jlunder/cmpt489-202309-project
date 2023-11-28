package synth.algorithms;

import synth.algorithms.lia.*;
import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class AstSynthesizer implements ISynthesizer {

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        var solver = new LinearSolver();
        Equation[] eqns = {
                new Equation(List.of(
                        new Term("a", 1),
                        new Term("b", 2)), 6),
                new Equation(List.of(
                        new Term("a", 2),
                        new Term("b", 1)), 6),
                new Equation(List.of(
                        new Term("a", 3),
                        new Term("b", 3)), 12),
        };
        var solSpace = solver.solve(eqns);
        var soln = solSpace.getSolution(0);
        for (int i = 0; i < solSpace.numTerms(); ++i) {
            System.out.println(solSpace.getTerm(i) + " = " + soln.get(i));
        }
        return null;
    }

}
