package synth.algorithms;

import synth.algorithms.lia.*;
import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class AstSynthesizer implements ISynthesizer {
    Map<Example, SolutionSet> computeSolutionSets(List<Example> examples) {
        LinearSolver linSolv = new LinearSolver(LinearSolver.makeAllTerms(3));
        try {
            HashMap<Example, SolutionSet> sets = new HashMap<>();
            for (var e : examples) {
                var sol = linSolv.solve(List.of(e));
                sets.put(e, sol);
                // for (int j = 0; j < sol.numSolutions(); ++j) {
                //     var soln = sol.getSolution(j);
                //     System.out.println("Solution " + j + ":");
                //     for (int i = 0; i < sol.numTerms(); ++i) {
                //         System.out.println("  " + sol.getTerm(i) + " = " + soln.get(i));
                //     }
                //     System.out.println();
                // }
            }
            return sets;
        } finally {
            linSolv.close();
        }
    }

    //void candidateGroupings()

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        // pseudocode steps:
        // partitions = partition(examples);
        // recognizedPartitions = developRecognizers(partitions);
        // program = reifyBestSolution(recognizedPartitions);
        // assert verify(program)
        // return program;
       return    null;
    }

}
