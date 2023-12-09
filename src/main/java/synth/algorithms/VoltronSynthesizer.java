package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.classify.*;
import synth.algorithms.lia.*;
import synth.algorithms.mcmc.*;
import synth.algorithms.representation.ExprRepresentation;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;
import synth.core.Program;
import synth.dsl.*;

import java.util.*;
import java.util.function.Function;
import java.util.logging.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(VoltronSynthesizer.class.getName());

    private Xoshiro256SS rng = new Xoshiro256SS(8383);
    private LinearSolver linSolv = new ORToolsCPLinearSolver(rng.nextSubsequence());

    private Collection<Discriminator> generateDiscriminators(Set<Example> allExamples,
            Collection<PartialSolution> partialSolutions)
            throws InterruptedException {
        var suggestions = new ArrayList<Discriminator>();
        nextSolution: for (var sol : partialSolutions) {
            var classification = sol.application();

            var positive = generateDiscriminatorsMcmc(allExamples, classification);
            // Check if one of our generated discriminators happens to be perfect
            for (var d : positive) {
                if (d.classification().equals(classification)) {
                    // This one is perfect, just add it and stop now! No point in keeping any others
                    suggestions.add(d);
                    continue nextSolution;
                }
            }
            // Try generating the discriminator in the negative, in case that synthesis is
            // easier and generates a better or at least different partition
            var negative = generateDiscriminatorsMcmc(allExamples, classification.inverted());
            // Again, check if one of our generated discriminators happens to be perfect
            for (var d : negative) {
                if (d.classification().equalsInverted(classification)) {
                    // This one is perfect, just add it and stop now
                    suggestions.add(d);
                    continue nextSolution;
                }
            }
            // None of the discriminators is perfect, save them all and proceed
            suggestions.addAll(positive);
            suggestions.addAll(negative);
        }

        var minimal = new ArrayList<Discriminator>();

        for (var trialDiscrim : suggestions) {
            boolean foundPositive = false, foundNegative = false;
            for (var accepted : minimal) {
                if (!foundPositive && trialDiscrim.classification().equals(accepted.classification())) {
                    foundPositive = true;
                } else if (!foundNegative && trialDiscrim.classification().equalsInverted(accepted.classification())) {
                    foundNegative = true;
                }
                if (foundPositive && foundNegative) {
                    break;
                }
            }

            if (!foundPositive || !foundNegative) {
                var cond = Asts.optimizeBoolAst(trialDiscrim.condition().reifyAsBoolAst());
                var optimized = new Discriminator(cond, allExamples);
                assert optimized.classification().equals(trialDiscrim.classification());
                if (!foundPositive) {
                    minimal.add(optimized);
                }
                if (!foundNegative) {
                    var inverted = new Discriminator(
                            cond instanceof NotNode ? (BoolNode) cond.child(0) : new NotNode(cond),
                            allExamples);
                    assert inverted.classification().equalsInverted(optimized.classification());
                    minimal.add(inverted);
                }
            }
        }

        return minimal;
    }

    private static final Function<Symbol[], Boolean> validateFunction(Classification desiredClassification) {
        return (x) -> {
            boolean falsePositive = false;
            boolean truePositive = false;
            boolean falseNegative = false;
            boolean trueNegative = false;
            for (var e : desiredClassification.included()) {
                if (Semantics.evaluateBoolPostOrder(x, e.input())) {
                    truePositive = true;
                } else {
                    falseNegative = true;
                }
            }
            for (var e : desiredClassification.excluded()) {
                if (Semantics.evaluateBoolPostOrder(x, e.input())) {
                    truePositive = true;
                } else {
                    falseNegative = true;
                }
            }

            // Discriminator must be over- or under-approximate: it has to identify at
            // least one category of things, and provide some information about the other,
            // to be useful.
            return truePositive && trueNegative && (falsePositive != falseNegative);
        };
    }

    private Collection<Discriminator> generateDiscriminatorsMcmc(Set<Example> allExamples,
            Classification desiredClassification)
            throws InterruptedException {
        McmcProgramOptimizer discriminatorOptimizer = new McmcProgramOptimizer(rng.nextSubsequence());

        final var maxProgramSize = 20;

        var initialX = discriminatorOptimizer.makeRandomized(20, McmcProgramOptimizer.CONDITION_SYMBOLS);
        var generateFrom = discriminatorOptimizer.generateFromFunction(McmcProgramOptimizer.CONDITION_SYMBOLS);
        var validate = validateFunction(desiredClassification);
        var approximateCost = McmcProgramOptimizer.boolSizeCostDecoratorFunction(
                McmcProgramOptimizer.confusionCostFunction(desiredClassification, 1f, 1f), 1f / 20f);
        var overApproximateCost = McmcProgramOptimizer.boolSizeCostDecoratorFunction(
                McmcProgramOptimizer.confusionCostFunction(desiredClassification, 0.1f, 1f), 1f / maxProgramSize);
        var underApproximateCost = McmcProgramOptimizer.boolSizeCostDecoratorFunction(
                McmcProgramOptimizer.confusionCostFunction(desiredClassification, 1f, 0.1f), 1f / maxProgramSize);
        var overApproximateResult = discriminatorOptimizer.optimize(initialX, generateFrom, overApproximateCost, 0.5f,
                validate, 1000000);
        var underApproximateResult = discriminatorOptimizer.optimize(initialX, generateFrom, underApproximateCost, 0.5f,
                validate, 1000000);

        // Update initialX to the over-approximate or under-approximate optimized result
        // if they're clearly at least an okay starting point
        if (overApproximateResult.bestIsValid() && (!underApproximateResult.bestIsValid()
                || (overApproximateResult.bestCost() < underApproximateResult.bestCost()))) {
            initialX = overApproximateResult.bestX();
        }
        if (underApproximateResult.bestIsValid() && (!overApproximateResult.bestIsValid()
                || (underApproximateResult.bestCost() < overApproximateResult.bestCost()))) {
            initialX = overApproximateResult.bestX();
        }

        var result = discriminatorOptimizer.optimize(initialX, generateFrom, approximateCost, 0.5f, validate, 1000000);

        var discriminators = new ArrayList<Discriminator>();
        if (overApproximateResult.bestIsValid()) {
            var boolParse = Semantics.makeParseTreeFromBoolPostOrder(result.bestX());
            var condition = Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(boolParse));
            discriminators.add(new Discriminator(condition, allExamples));
        }
        if (underApproximateResult.bestIsValid()) {
            var boolParse = Semantics.makeParseTreeFromBoolPostOrder(result.bestX());
            var condition = Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(boolParse));
            discriminators.add(new Discriminator(condition, allExamples));
        }
        if (result.bestIsValid()) {
            var boolParse = Semantics.makeParseTreeFromBoolPostOrder(result.bestX());
            var condition = Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(boolParse));
            discriminators.add(new Discriminator(condition, allExamples));
        }

        // Discriminator computes the actual classification
        return discriminators;
    }

    private ExprRepresentation buildDecisionTreeAstFromPartialSolutions(Set<Example> allExamples,
            Collection<PartialSolution> partialSolutions, Collection<Discriminator> discriminators)
            throws InterruptedException {
        McmcDecisionTreeOptimizer decisionTreeOptimizer = new McmcDecisionTreeOptimizer(rng.nextSubsequence(),
                partialSolutions.size() * 4, partialSolutions, discriminators, allExamples);
        var res = decisionTreeOptimizer.optimize(1000000);
        if (!res.bestIsValid()) {
            logger.log(Level.WARNING, "Unable to produce valid decision tree");
            return null;
        }
        return res.bestX().reifyAsDecisionTree();
    }

    private ExprNode synthesizeAst(List<Example> examples) throws InterruptedException {
        var allExamples = Set.copyOf(examples);
        var partialSolutions = linSolv.computeSolutionSets(examples);
        if (partialSolutions == null) {
            return null;
        }
        // Check if any of our solution sets cover the whole space, and early-out if so!
        for (var sol : partialSolutions) {
            if (sol.application().excluded().isEmpty()) {
                // Trivial solution!
                return sol.solution().reifyAsExprAst();
            }
        }
        var discriminators = generateDiscriminators(allExamples, partialSolutions);
        var decisionTree = buildDecisionTreeAstFromPartialSolutions(allExamples, partialSolutions, discriminators);
        if (decisionTree == null) {
            return null;
        }
        return Asts.optimizeExprAst(decisionTree.reifyAsExprAst());
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
            var ast = synthesizeAst(examples);
            if (ast == null) {
                return null;
            }
            return new Program(ast.reify());
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupted during synthesize()");
            return null;
        }
    }

}
