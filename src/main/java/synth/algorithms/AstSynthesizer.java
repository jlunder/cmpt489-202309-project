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
        LinearSolver linSolv = new LinearSolver(LinearSolver.makeAllTerms(3, 3, 3));
        try {
            var solSpace = linSolv.solve(examples);
            for (int j = 0; j < solSpace.numSolutions(); ++j) {
                var soln = solSpace.getSolution(j);
                System.out.println("Solution " + j + ":");
                for (int i = 0; i < solSpace.numTerms(); ++i) {
                    System.out.println("  " + solSpace.getTerm(i) + " = " + soln.get(i));
                }
                System.out.println();
            }
            return null;
        } finally {
            linSolv.close();
        }
    }

}
