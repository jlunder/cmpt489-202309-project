package synth.algorithms.lia;

import java.util.*;
import java.util.logging.*;

import synth.algorithms.classify.Classification;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;

public abstract class LinearSolver {
    private static Logger logger = Logger.getLogger(LinearSolver.class.getName());

    public static class SolveResult {
        private boolean solutionsAreDefinite;
        private List<LinearSolution> solutions;

        public boolean solutionsAreDefinite() {
            return solutionsAreDefinite;
        }

        public List<LinearSolution> solutions() {
            return solutions;
        }

        SolveResult(boolean hasDefiniteSolutions, List<LinearSolution> solutions) {
            this.solutionsAreDefinite = hasDefiniteSolutions;
            this.solutions = solutions;
        }
    }

    private Xoshiro256SS rng;

    protected Xoshiro256SS rng() {
        return rng;
    }

    public LinearSolver(Xoshiro256SS rng) {
        this.rng = rng;
    }

    protected ArrayList<Example> randomOrder(Collection<Example> examples) {
        var scrambled = new ArrayList<Example>(examples);
        var len = examples.size();
        for (int i = 0; i < len; ++i) {
            var j = rng.nextInt(len);
            var tmp = scrambled.get(j);
            scrambled.set(j, scrambled.get(i));
            scrambled.set(i, tmp);
        }
        return scrambled;
    }

    List<Integer> subsetSizeSchedule = List.of(8, 4, 4, 2, 2, 1);

    /**
     * For each example, compute a solution set using the linear solver.
     */
    public Map<LinearSolution, Classification> computeSolutionSets(List<Example> examples) throws InterruptedException {
        var uncoveredExamples = new HashSet<Example>(examples);
        var solutions = new HashMap<LinearSolution, Classification>();

        while (uncoveredExamples.size() > 0) {
            logger.log(Level.INFO, "Discovering sub-solutions: {0} examples not covered yet",
                    new Object[] { uncoveredExamples.size()  });
            LinearSolution sol = null;
            // Pick some UNCOVERED example -- how about the first?
            var seedExample = uncoveredExamples.iterator().next();
            var examplePool = List.copyOf(uncoveredExamples);
            for (var subsetSize : subsetSizeSchedule) {
                if (subsetSize > examplePool.size()) {
                    continue;
                }
                var subset = new HashSet<Example>();
                subset.add(seedExample);
                // Duplicates won't increase subset.size()
                while (subset.size() < subsetSize) {
                    subset.add(examplePool.get(rng.nextInt(examplePool.size())));
                }
                // Try to find a solution
                sol = solveSubset(subset);
                if (sol != null) {
                    break;
                }
            }
            if (sol == null) {
                // Didn't find anything with the linear solver at any size? we'll just have to
                // try the degenerate solution
                if (seedExample.output() > 0) {
                    sol = new LinearSolution(Map.of(Term.TERM_1, seedExample.output()));
                    logger.log(Level.WARNING, "Degenerate solution for example: {0}", new Object[] { seedExample });
                    solutions.put(sol, Classification.makeFromExamples(sol, examples));
                } else {
                    // Something is fishy, anyway, we probably just can't solve this one?
                    logger.log(Level.WARNING, "Unable to find solution for example: {0}", new Object[] { seedExample });
                    return null;
                }
                uncoveredExamples.remove(seedExample);
            } else {
                var subset = new HashSet<Example>();
                completeGroupUsingSolutions(List.of(sol), examples, subset);
                uncoveredExamples.removeAll(subset);
                solutions.put(sol, Classification.makeFromExamples(sol, examples));
            }
        }
        return solutions;
    }

    protected LinearSolution solveSubset(Collection<Example> exampleSubset) {
        throw new UnsupportedOperationException("Not implemented");
    }

    protected SolveResult completeGroupUsingSolutions(Collection<LinearSolution> solutions,
            Collection<Example> examples, HashSet<Example> included) {
        var ungroupedExamples = new HashSet<Example>(examples);
        ungroupedExamples.removeAll(included);
        var viableSolutions = new HashSet<LinearSolution>(solutions);
        for (var e : examples) {
            boolean any = false, all = true;
            for (var s : viableSolutions) {
                var compatible = (s.evalExpr(e.input()) == e.output());
                any |= compatible;
                all &= compatible;
            }
            if (any) {
                included.add(e);
                ungroupedExamples.remove(e);
                if (!all) {
                    for (var s : List.copyOf(viableSolutions)) {
                        if (s.evalExpr(e.input()) != e.output()) {
                            viableSolutions.remove(s);
                        }
                    }
                }
            }
        }
        logger.log(Level.INFO, "-- Fast grouping accepted {0}/{1} examples, {2} solutions winnowed to {3}",
                new Object[] { included.size(), examples.size(), solutions.size(), viableSolutions.size() });
        return new SolveResult(true, List.copyOf(viableSolutions));
    }

}
