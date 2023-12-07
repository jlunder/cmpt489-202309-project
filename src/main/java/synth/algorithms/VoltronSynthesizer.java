package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.classify.*;
import synth.algorithms.lia.*;
import synth.algorithms.mcmc.*;
import synth.algorithms.representation.ExprRepresentation;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;
import synth.dsl.*;

import java.util.*;
import java.util.logging.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(VoltronSynthesizer.class.getName());

    private Xoshiro256SS rng = new Xoshiro256SS(8383);
    private LinearSolver linSolv = new ORToolsCPLinearSolver(rng.nextSubsequence());

    private Collection<Discriminator> generateDiscriminators(Set<Example> allExamples,
            Collection<PartialSolution> partialSolutions)
            throws InterruptedException {
        var suggestions = new ArrayList<Discriminator>();
        for (var sol : partialSolutions) {
            var classification = sol.application();

            var positive = generateImperfectDiscriminator(allExamples, classification);
            suggestions.add(positive);
            // If this one is perfect already, don't keep generating!
            if (positive.classification().equals(classification)) {
                continue;
            }
            // Try generating the discriminator in the negative, in case that synthesis is
            // easier and generates a better or at least different partition
            var negative = generateImperfectDiscriminator(allExamples, classification.inverted());
            suggestions.add(negative);
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

    private Discriminator generateImperfectDiscriminator(Set<Example> allExamples, Classification desiredClassification)
            throws InterruptedException {
        McmcProgramOptimizer discriminatorOptimizer = new McmcProgramOptimizer(rng.nextSubsequence(),
                McmcProgramOptimizer.confusionCostFunction(desiredClassification, rng.nextSubsequence(), 50),
                McmcProgramOptimizer.CONDITION_SYMBOLS);

        var result = discriminatorOptimizer.optimize(discriminatorOptimizer.makeRandomized(20), 10, (x) -> {
            return true;
        }, 1000000);
        var boolParse = Semantics.makeBoolParseTreeFromPostOrder(result.bestX());
        var condition = Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(boolParse));

        // Discriminator computes the actual classification
        return new Discriminator(condition, allExamples);
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
