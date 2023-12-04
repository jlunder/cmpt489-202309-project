package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.classify.*;
import synth.algorithms.lia.*;
import synth.algorithms.mcmc.McmcProgramOptimizer;
import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private static class PartialSolution {
        private LinearSolution solution;
        private Classification application;
        private List<Discriminator> positiveDiscriminators;
        private List<Discriminator> negativeDiscriminators;

        public LinearSolution solution() {
            return solution;
        }

        public Classification application() {
            return application;
        }

        public List<Discriminator> positiveDiscriminators() {
            return positiveDiscriminators;
        }

        public List<Discriminator> negativeDiscriminators() {
            return negativeDiscriminators;
        }

        public PartialSolution(LinearSolution solution, Classification application,
                List<Discriminator> positiveDiscriminators, List<Discriminator> negativeDiscriminators) {
            this.solution = solution;
            this.application = application;
            this.positiveDiscriminators = positiveDiscriminators;
            this.negativeDiscriminators = negativeDiscriminators;
        }
    }

    private LinearSolver linSolv = new LinearSolver(LinearSolver.makeAllTerms(2), 3 * 3);

    private Set<Example> allExamples;
    private Map<SolutionSet, Classification> linearSolutionSets;
    private List<Discriminator> discriminatorPool;
    private List<PartialSolution> approximatePartialSolutions;

    private void reset() {
        allExamples = null;
        linearSolutionSets = null;
        discriminatorPool = null;
        approximatePartialSolutions = null;
    }

    private List<Discriminator> generateDiscriminatorPool() throws InterruptedException {
        var discriminators = new ArrayList<Discriminator>();
        for (var sol : linearSolutionSets.entrySet()) {
            var classification = sol.getValue();

            var positive = generateImperfectDiscriminator(
                    new Classification(classification.included(), classification.excluded()));
            if (positive.classification().included().equals(classification.included())) {
                // If this one is perfect already, don't keep generating!
                discriminators.add(positive);
                continue;
            }
            var negative = generateImperfectDiscriminator(
                    new Classification(classification.excluded(), classification.included()));
            discriminators.add(positive);
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
        McmcProgramOptimizer discriminatorOptimizer = new McmcProgramOptimizer(2532,
                McmcProgramOptimizer.classificationCostFunction(desiredClassification),
                McmcProgramOptimizer.CONDITION_SYMBOLS);

        var result = discriminatorOptimizer.optimize(new Symbol[20], 0.5f, 10000);
        var boolParse = Semantics.makeBoolParseTreeFromPostOrder(result.bestX());
        var condition = Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(boolParse));

        // Discriminator computes the actual classification
        return new Discriminator(condition, allExamples);
    }

    // public int scoreProgram(AstNode program);

    private class RankedSolutions implements Comparable<RankedSolutions> {
        private int ranking;
        private SolutionSet solutions;
        private HashSet<Example> coveredExamples;

        // public int ranking() {
        // return ranking;
        // }

        public SolutionSet solutions() {
            return solutions;
        }

        public HashSet<Example> coveredExamples() {
            return coveredExamples;
        }

        public RankedSolutions(SolutionSet solutions, HashSet<Example> coveredExamples) {
            this.solutions = solutions;
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
                res = Integer.compare(solutions.hashCode(), other.solutions.hashCode());
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
            return ranking == rsObj.ranking && solutions.equals(rsObj.solutions)
                    && coveredExamples.equals(rsObj.coveredExamples);
        }

        @Override
        public int hashCode() {
            return (ranking * 1056019 + solutions.hashCode()) * 1056019 + coveredExamples.hashCode();
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
        assert !linearSolutionSets.isEmpty();
        assert !discriminatorPool.isEmpty();

        var choice = new ArrayList<PartialSolution>();

        var uncoveredExamples = new HashSet<Example>(allExamples);
        var usefulSols = new HashSet<RankedSolutions>();
        var usefulDiscrims = new HashMap<Discriminator, HashSet<Example>>();
        var usedDiscrims = new ArrayList<Discriminator>();

        // Initialize useful solutions with all available solutions
        for (var sol : linearSolutionSets.entrySet()) {
            usefulSols.add(new RankedSolutions(sol.getKey(), new HashSet<>(sol.getValue().included())));
        }
        for (var discrim : discriminatorPool) {
            usefulDiscrims.put(discrim, new HashSet<>(discrim.classification().included()));
        }

        while (!usefulSols.isEmpty() && !usedDiscrims.isEmpty()) {
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
            var partial = new PartialSolution(bestCoveredSolutions.solutions().representativeSolution(),
                    linearSolutionSets.get(bestCoveredSolutions.solutions()), List.of(bestDiscrim),
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
    private ExprNode buildDecisionTreeAstFromPartialSolutions() {
        assert "Not ready yet!" == null;
        return null;
    }

    private ExprNode buildAstFromTerm(int coeff, int xOrder, int yOrder, int zOrder) {
        assert coeff > 0 && xOrder >= 0 && yOrder >= 0 && zOrder >= 0;
        var children = new ArrayList<ExprNode>();
        if (coeff > 1) {
            children.add(new ExprConstNode(coeff));
        }
        for (int i = 0; i < xOrder; ++i) {
            children.add(VariableNode.VAR_X);
        }
        for (int i = 0; i < yOrder; ++i) {
            children.add(VariableNode.VAR_Y);
        }
        for (int i = 0; i < zOrder; ++i) {
            children.add(VariableNode.VAR_Z);
        }

        // Special case: if this is a unit term, we will have not added *any* children
        if (children.size() == 0) {
            return new ExprConstNode(1);
        } else if (children.size() == 1) {
            return children.get(0);
        } else {
            return new MultiplyNode(children.toArray(ExprNode[]::new));
        }
    }

    private ExprNode buildAstFromLinearSolution(LinearSolution solution) {
        assert solution.coefficients().size() > 0;
        var children = new ArrayList<ExprNode>();
        for (var termC : solution.coefficients().entrySet()) {
            children.add(buildAstFromTerm(termC.getValue(), termC.getKey().xPower(), termC.getKey().yPower(),
                    termC.getKey().zPower()));
        }
        if (children.size() == 1) {
            return children.get(0);
        } else {
            return new AddNode(children.toArray(ExprNode[]::new));
        }
    }

    private ExprNode synthesizeAst(List<Example> examples) throws InterruptedException {
        allExamples = Set.copyOf(examples);
        linearSolutionSets = linSolv.computeSolutionSets(examples);
        // Check if any of our solution sets cover the whole space, and early-out if so!
        for (var e : linearSolutionSets.entrySet()) {
            if (e.getValue().excluded().isEmpty()) {
                // Trivial solution!
                return buildAstFromLinearSolution(e.getKey().representativeSolution());
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
                var program = new Program(ast.reify());
                assert validate(examples, program);
                return program;
            } catch (InterruptedException e) {
                return null;
            }
        } finally {
            reset();
            linSolv.close();
        }
    }

}
