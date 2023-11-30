package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.lia.*;
import synth.core.*;

import java.util.*;

public class AstSynthesizer extends SynthesizerBase {
    // for (int j = 0; j < sol.numSolutions(); ++j) {
    // var soln = sol.getSolution(j);
    // System.out.println("Solution " + j + ":");
    // for (int i = 0; i < sol.numTerms(); ++i) {
    // System.out.println(" " + sol.getTerm(i) + " = " + soln.get(i));
    // }
    // System.out.println();
    // }

    /**
     * For each example, compute a solution set using the linear solver.
     */
    private Map<Example, SolutionSet> computeSolutionSets(List<Example> examples) {
        LinearSolver linSolv = new LinearSolver(LinearSolver.makeAllTerms(3));
        try {
            HashMap<Example, SolutionSet> sets = new HashMap<>();
            for (var e : examples) {
                var sol = linSolv.solve(List.of(e));
                sets.put(e, sol);
            }
            return sets;
        } finally {
            linSolv.close();
        }
    }

    /**
     * Find all distinct solutions in the provided sets, and use them to identify
     * subsets of the examples which have common solutions.
     */
    private Map<SolutionSet, Set<Example>> groupSolutions(Map<Example, SolutionSet> solutions) {
        var examples = solutions.keySet();
        return null;
    }

    /**
     * Find a subset of solution sets which (in descending order of importance)
     * 1. definitely covers all examples,
     * 2. is distinguishable with minimum exact matching, and
     * 3. minimizes the number of distinct solutions.
     */
    private Map<SolutionSet, Set<Example>> minimizeSolutionCover(Map<SolutionSet, Set<Example>> groups) {
        return null;
    }

    /**
     * Build a program which implements the solutions in the solution cover.
     */
    private ExprNode buildAstFromSolutions(Map<SolutionSet, Set<Example>> cover) {
        return null;
    }

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
        var allSolutions = computeSolutionSets(examples);
        var solutionGroups = groupSolutions(allSolutions);
        var cover = minimizeSolutionCover(solutionGroups);
        var ast = buildAstFromSolutions(cover);
        var program = new Program(ast.reify());
        assert validate(examples, program);
        return program;
    }

}
