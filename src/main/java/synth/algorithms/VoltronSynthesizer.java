package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.lia.*;
import synth.algorithms.voltron.*;
import synth.core.*;

import java.util.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private LinearSolver linSolv = new LinearSolver(LinearSolver.makeAllTerms(2), 3*3);
    private SolutionBlackboard bb = new SolutionBlackboard();

    /**
     * Build a program which implements the solutions in the solution cover.
     */
    private ExprNode buildAstFromSolutions(Map<SolutionSet, Set<Example>> cover) {
        int solsN = 1;
        for (var sols : cover.entrySet()) {
            System.out.println("solution set " + solsN + ":");
            ++solsN;

            int solN = 1;
            for (var sol : sols.getKey().solutions()) {
                System.out.println("  solution " + solN + ":");
                ++solN;

                for (var termC : sol.coefficients().entrySet()) {
                    System.out.println(
                            "    " + (termC.getValue() > 1 ? termC.getValue() + " * " : "") + termC.getKey());
                }
                System.out.println();
            }
            System.out.println("  examples:");
            for (var ex : sols.getValue()) {
                System.out.println("    " + ex);
            }
        }

        return new ExprConstNode(1);
    }

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        try {
            // pseudocode steps:
            // partitions = partition(examples);
            // recognizedPartitions = developRecognizers(partitions);
            var solutions = linSolv.computeSolutionSets(examples);
            // var solutionGroups = groupExamplesBySolution(solutions);
            var cover = bb.minimizeSolutionCover(solutions);
            var ast = buildAstFromSolutions(cover);
            var program = new Program(ast.reify());
            assert validate(examples, program);
            return program;
        } finally {
            linSolv.close();
        }
    }

}
