package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.lia.*;
import synth.core.*;

import java.util.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private LinearSolver linSolv = new LinearSolver(LinearSolver.makeAllTerms(2), 2);
    private static final boolean logGroups = false;

    /**
     * For each example, compute a solution set using the linear solver.
     */
    private Map<SolutionSet, Set<Example>> computeSolutionSets(List<Example> examples) {
        var sets = new HashMap<SolutionSet, Set<Example>>();
        assert examples.size() > 0;
        int i = 0, j;
        while (i < examples.size()) {
            if (logGroups) {
                System.out.println("i = " + i);
            }
            var sess = linSolv.startSession();
            Example ei = examples.get(i);
            sess.addEquation(ei);
            j = i + 1;
            while (j < examples.size()) {
                if (logGroups) {
                    System.out.println("  j = " + j);
                }
                Example ej = examples.get(j);
                sess.addEquation(ej);
                if (!sess.checkSatisfiable()) {
                    if (logGroups) {
                        System.out.println("  UNSAT");
                    }
                    break;
                }
                ++j;
            }
            var exs = new HashSet<Example>(j - i);
            sess = linSolv.startSession();
            for (int k = i; k < j; ++k) {
                var ek = examples.get(k);
                sess.addEquation(ek);
                exs.add(ek);
            }
            var sols = sess.solve();
            assert !sols.isEmpty();
            if (logGroups) {
                for (var sol : sols.solutions()) {
                    System.out.println("  solution:");
                    for (var termC : sol.coefficients().entrySet()) {
                        System.out.println(
                                "    " + (termC.getValue() > 1 ? termC.getValue() + " * " : "") + termC.getKey());
                    }
                }
            }
            sets.put(sols, exs);
            i = j;
        }
        return sets;
    }

    /**
     * Find a subset of solution sets which (in descending order of importance)
     * 1. definitely covers all examples,
     * 2. is distinguishable with minimum exact matching, and
     * 3. minimizes the number of distinct solutions.
     */
    private Map<SolutionSet, Set<Example>> minimizeSolutionCover(Map<SolutionSet, Set<Example>> groups) {
        return groups;
    }

    private ExprNode synthesizeClassifier(Map<Integer, Set<Example>> categories) {
        return null;
    }

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
            var solutions = computeSolutionSets(examples);
            // var solutionGroups = groupExamplesBySolution(solutions);
            var cover = minimizeSolutionCover(solutions);
            var ast = buildAstFromSolutions(cover);
            var program = new Program(ast.reify());
            assert validate(examples, program);
            return program;
        } finally {
            linSolv.close();
        }
    }

}
