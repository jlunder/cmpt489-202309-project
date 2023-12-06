package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.classify.*;
import synth.algorithms.lia.*;
import synth.algorithms.mcmc.*;
import synth.algorithms.representation.*;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;
import synth.dsl.*;

import java.util.*;
import java.util.logging.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(VoltronSynthesizer.class.getName());

    private Xoshiro256SS rng = new Xoshiro256SS(8383);
    private LinearSolver linSolv = new ORToolsCPLinearSolver(rng.nextSubsequence());

    private Set<Example> allExamples;
    private Map<? extends ExprRepresentation, Classification> trialSolutions;
    private List<Discriminator> discriminatorPool;
    private List<PartialSolution> approximatePartialSolutions;

    private void reset() {
        allExamples = null;
        trialSolutions = null;
        discriminatorPool = null;
        approximatePartialSolutions = null;
    }

    private List<Discriminator> generateDiscriminatorPool() throws InterruptedException {
        var discriminators = new ArrayList<Discriminator>();
        for (var sol : trialSolutions.entrySet()) {
            var classification = sol.getValue();

            var positive = generateImperfectDiscriminator(
                    new Classification(classification.included(), classification.excluded()));
            discriminators.add(positive);
            if (positive.classification().included().equals(classification.included())) {
                // If this one is perfect already, don't keep generating!
                continue;
            }
            var negative = generateImperfectDiscriminator(
                    new Classification(classification.excluded(), classification.included()));
            discriminators.add(negative);
        }

        for (var discrim : List.copyOf(discriminators)) {
            var cond = Asts.optimizeBoolAst(discrim.condition());
            var optimized = new Discriminator(cond, allExamples);
            discriminators.add(optimized);
            var inverted = new Discriminator(cond instanceof NotNode ? (BoolNode) cond.child(0) : new NotNode(cond),
                    allExamples);
            discriminators.add(inverted);
        }

        return discriminators;
    }

    private Discriminator generateImperfectDiscriminator(Classification desiredClassification)
            throws InterruptedException {
        McmcProgramOptimizer discriminatorOptimizer = new McmcProgramOptimizer(rng.nextSubsequence(),
                McmcProgramOptimizer.confusionCostFunction(desiredClassification, rng.nextSubsequence(), 50),
                McmcProgramOptimizer.CONDITION_SYMBOLS);

        var result = discriminatorOptimizer.optimize(new Symbol[20], 0.5f, 10000);
        var boolParse = Semantics.makeBoolParseTreeFromPostOrder(result.bestX());
        var condition = Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(boolParse));

        // Discriminator computes the actual classification
        return new Discriminator(condition, allExamples);
    }

    private class RankedSolutions implements Comparable<RankedSolutions> {
        private int ranking;
        private ExprRepresentation solution;
        private HashSet<Example> coveredExamples;

        // public int ranking() {
        // return ranking;
        // }

        public ExprRepresentation solution() {
            return solution;
        }

        public HashSet<Example> coveredExamples() {
            return coveredExamples;
        }

        public RankedSolutions(ExprRepresentation solution, HashSet<Example> coveredExamples) {
            this.solution = solution;
            this.coveredExamples = coveredExamples;
            ranking = coveredExamples.size();
        }

        public void removeExamples(Collection<Example> toRemove) {
            coveredExamples.removeAll(toRemove);
            ranking = coveredExamples.size();
        }

        @Override
        public int compareTo(RankedSolutions other) {
            // higher rankings sort first
            int res = Integer.compare(other.ranking, ranking);

            // ranking is all that really matters, the rest is just tie-breakers to make the
            // sorting stable
            if (res == 0) {
                res = Integer.compare(solution.hashCode(), other.solution.hashCode());
            }
            if (res == 0) {
                res = Integer.compare(coveredExamples.hashCode(), other.coveredExamples.hashCode());
            }

            return res;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !(obj instanceof RankedSolutions)) {
                return false;
            }
            RankedSolutions rsObj = (RankedSolutions) obj;
            return ranking == rsObj.ranking && solution.equals(rsObj.solution)
                    && coveredExamples.equals(rsObj.coveredExamples);
        }

        @Override
        public int hashCode() {
            return (ranking * 1056019 + solution.hashCode()) * 1056019 + coveredExamples.hashCode();
        }

    }

    /**
     * Find a subset of solution sets which (in descending order of importance)
     * 1. covers as many examples as practical,
     * 2. is distinguishable with minimum exact matching, and
     * 3. minimizes the number of distinct solutions.
     */
    private List<PartialSolution> chooseApproximatePartialSolutions() {
        assert !allExamples.isEmpty();
        assert !trialSolutions.isEmpty();
        assert !discriminatorPool.isEmpty();

        var choice = new ArrayList<PartialSolution>();

        var uncoveredExamples = new HashSet<Example>(allExamples);
        var usefulSols = new HashSet<RankedSolutions>();
        var usefulDiscrims = new HashMap<Discriminator, HashSet<Example>>();
        var usedDiscrims = new ArrayList<Discriminator>();

        // Initialize useful solutions with all available solutions
        for (var sol : trialSolutions.entrySet()) {
            usefulSols.add(new RankedSolutions(sol.getKey(), new HashSet<>(sol.getValue().included())));
        }
        for (var discrim : discriminatorPool) {
            usefulDiscrims.put(discrim, new HashSet<>(discrim.classification().included()));
        }

        while (!usefulSols.isEmpty() && !usefulDiscrims.isEmpty()) {
            // Find a solution set which is (a) as well-covered as possible and (b) as large
            // as possible
            var rankedSols = new PriorityQueue<RankedSolutions>(usefulSols);
            RankedSolutions bestCoveredSolutions = null;
            Discriminator bestDiscrim = null;
            float bestCost = -1f;
            solSearch: while (!rankedSols.isEmpty()) {
                var sols = rankedSols.poll();

                // Check each discriminator
                for (var de : usefulDiscrims.entrySet()) {
                    float cost = setDifferenceCost(sols.coveredExamples(), de.getValue());
                    // Does this pair of discriminator and solution set partition our data well?
                    if (cost < bestCost || bestCoveredSolutions == null) {
                        bestCoveredSolutions = sols;
                        bestDiscrim = de.getKey();
                        bestCost = cost;
                    }
                    // Is this a perfect solution? Stop looking then!
                    if (bestCost <= 0f) {
                        break solSearch;
                    }
                }
            }
            assert bestCost >= 0f && bestCoveredSolutions != null && bestDiscrim != null;

            // Remove the solution we found, and all the examples it covers
            var partial = new PartialSolution(bestCoveredSolutions.solution(),
                    trialSolutions.get(bestCoveredSolutions.solution()), List.of(bestDiscrim),
                    List.copyOf(usedDiscrims));
            choice.add(partial);

            // Don't try this solution or discriminator again
            usefulSols.remove(bestCoveredSolutions);
            usefulDiscrims.remove(bestDiscrim);
            // add the discriminator to the list of suggestions used to EXCLUDE examples
            usedDiscrims.add(bestDiscrim);

            // Remove the examples we've categorized -- note that we're pretending here that
            // we have a perfect discriminator! This pass is intentionally approximate,
            // deficiencies will be fixed up later, so this presumption of perfect example
            // coverage will be justified at that point.
            uncoveredExamples.removeAll(bestCoveredSolutions.coveredExamples());

            // Are we done? -- if there's only one example left in our set, it can be the
            // last else case
            if (usefulSols.isEmpty() || uncoveredExamples.size() <= 1) {
                break;
            }

            // Update our useful lists, removing the examples we've covered so far
            for (var e : List.copyOf(usefulDiscrims.entrySet())) {
                e.getValue().removeAll(bestCoveredSolutions.coveredExamples());
                if (e.getValue().isEmpty()) {
                    usefulDiscrims.remove(e.getKey());
                }
            }
            for (var sol : List.copyOf(usefulSols)) {
                sol.removeExamples(bestCoveredSolutions.coveredExamples());
                if (sol.coveredExamples().isEmpty()) {
                    usefulSols.remove(sol);
                }
            }
        }

        return choice;
    }

    private float setDifferenceCost(Set<Example> a, Set<Example> b) {
        int commonCount = 0;
        var smaller = a;
        var larger = b;
        if (b.size() < a.size()) {
            smaller = b;
            larger = a;
        }
        for (var e : a) {
            if (b.contains(e)) {
                ++commonCount;
            }
        }
        // cost = inclusion errors + exclusion errors
        return (float) (smaller.size() + larger.size() - 2 * commonCount);
    }

    /**
     * Find a subset of solution sets which (in descending order of importance)
     * 1. covers as many examples as practical,
     * 2. is distinguishable with minimum exact matching, and
     * 3. minimizes the number of distinct solutions.
     */
    private ExprNode buildDecisionTreeAstFromPartialSolutions() throws InterruptedException {
        McmcDecisionTreeOptimizer decisionTreeOptimizer = new McmcDecisionTreeOptimizer(rng.nextSubsequence(),
                approximatePartialSolutions.size() * 2, approximatePartialSolutions.toArray(PartialSolution[]::new),
                allExamples);
        var x = decisionTreeOptimizer.new FlatDecisionTree();
        x.randomize();
        var lastCost = decisionTreeOptimizer.computeCost(x);
        var languishCount = 0;
        for (int i = 0; i < 100; ++i) {
            var res = decisionTreeOptimizer.optimize(x, 0.5f, 100000);
            var curCost = res.bestCost();
            x = res.bestX();
            logger.log(Level.INFO, "Decision tree optimization pass {0}, cost={1}", new Object[] { i, curCost });
            if (res.reachedTargetCost()) {
                logger.log(Level.INFO, "Decision tree optimization pass {0}, cost={1}", new Object[] { i, curCost });
            }
            if (curCost / lastCost > 0.97f) {
                ++languishCount;
                if (languishCount > 5) {
                    logger.log(Level.INFO, "Decision tree persistently not improving, giving up");
                    break;
                }
            } else {
                // Made new progress, reset
                languishCount = 0;
            }
        }
        return null;
    }

    private ExprNode synthesizeAst(List<Example> examples) throws InterruptedException {
        allExamples = Set.copyOf(examples);
        trialSolutions = linSolv.computeSolutionSets(examples);
        if (trialSolutions == null) {
            return null;
        }
        // Check if any of our solution sets cover the whole space, and early-out if so!
        for (var e : trialSolutions.entrySet()) {
            if (e.getValue().excluded().isEmpty()) {
                // Trivial solution!
                return e.getKey().reifyAsExprAst();
            }
        }
        discriminatorPool = generateDiscriminatorPool();
        approximatePartialSolutions = chooseApproximatePartialSolutions();
        return buildDecisionTreeAstFromPartialSolutions();
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
            try {
                var ast = synthesizeAst(examples);
                if (ast == null) {
                    return null;
                }
                var program = new Program(ast.reify());
                assert validate(examples, program);
                return program;
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Interrupted during synthesize()");
                return null;
            }
        } finally {
            reset();
        }
    }

}
