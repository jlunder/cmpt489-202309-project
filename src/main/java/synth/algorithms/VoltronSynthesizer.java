package synth.algorithms;

import synth.algorithms.ast.*;
import synth.algorithms.classify.*;
import synth.algorithms.enumeration.ProgramEnumerator;
import synth.algorithms.lia.*;
import synth.algorithms.mcmc.*;
import synth.algorithms.representation.ExprRepresentation;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Environment;
import synth.core.Example;
import synth.core.ParseNode;
import synth.core.Program;
import synth.dsl.*;

import java.util.*;
import java.util.logging.*;

public class VoltronSynthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(VoltronSynthesizer.class.getName());

    private Xoshiro256SS rng = new Xoshiro256SS(8383);
    private LinearSolver linSolv = new ORToolsCPLinearSolver(rng.nextSubsequence());

    private Collection<Discriminator> generateDiscriminators(Set<Environment> allInputs,
            Collection<PartialSolution> partialSolutions)
            throws InterruptedException {
        var suggestions = new ArrayList<Discriminator>();
        nextSolution: for (var sol : partialSolutions) {
            var classification = sol.application();

            var positive = generateDiscriminatorsEnum(allInputs, classification);
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
            var negative = generateDiscriminatorsEnum(allInputs, classification.inverted());
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
                var optimized = new Discriminator(cond, allInputs);
                assert optimized.classification().equals(trialDiscrim.classification());
                if (!foundPositive) {
                    minimal.add(optimized);
                }
                if (!foundNegative) {
                    var inverted = new Discriminator(
                            cond instanceof NotNode ? (BoolNode) cond.child(0) : new NotNode(cond),
                            allInputs);
                    assert inverted.classification().equalsInverted(optimized.classification());
                    minimal.add(inverted);
                }
            }
        }

        return minimal;
    }

    private class QuadrantEvaluation {
        Classification desiredClassification;
        private boolean perfect;
        private boolean overApproximate;
        private boolean underApproximate;
        private HashSet<Environment> positiveErrors = new HashSet<>();
        private HashSet<Environment> negativeErrors = new HashSet<>();

        public boolean isPerfect() {
            return perfect;
        }

        public boolean isOverApproximate() {
            return overApproximate;
        }

        public boolean isUnderApproximate() {
            return underApproximate;
        }

        public Set<Environment> positiveErrors() {
            return positiveErrors;
        }

        public Set<Environment> negativeErrors() {
            return negativeErrors;
        }

        public QuadrantEvaluation(Classification desiredClassification) {
            this.desiredClassification = desiredClassification;
        }

        public boolean evaluate(ParseNode condition) {
            perfect = false;
            overApproximate = false;
            underApproximate = false;
            positiveErrors.clear();
            negativeErrors.clear();
            boolean falsePositive = false, truePositive = false, falseNegative = false, trueNegative = false;
            var e = desiredClassification.included().iterator();
            var f = desiredClassification.excluded().iterator();
            while (e.hasNext() || f.hasNext()) {
                if (e.hasNext()) {
                    var env = e.next();
                    if (Semantics.evaluateBool(condition, env)) {
                        truePositive = true;
                    } else {
                        if (falsePositive) {
                            return false;
                        }
                        negativeErrors.add(env);
                        falseNegative = true;
                    }
                } else if (!truePositive) {
                    // If it hasn't demonstrated truePositive by now it can't
                    return false;
                }
                if (f.hasNext()) {
                    var env = f.next();
                    if (Semantics.evaluateBool(condition, env)) {
                        if (falseNegative) {
                            return false;
                        }
                        positiveErrors.add(env);
                        falsePositive = true;
                    } else {
                        trueNegative = true;
                    }
                } else if (!trueNegative) {
                    // If it hasn't demonstrated trueNegative by now it can't
                    return false;
                }
            }
            perfect = !falsePositive && !falseNegative;
            overApproximate = !falseNegative;
            underApproximate = !falsePositive;
            return truePositive && trueNegative && !(falsePositive && falseNegative);
        }
    }

    private Collection<Discriminator> generateDiscriminatorsEnum(Set<Environment> allInputs,
            Classification desiredClassification) throws InterruptedException {
        var discriminators = new ArrayList<Discriminator>();
        var overApproximationErrors = new HashSet<HashSet<Environment>>();
        var underApproximationErrors = new HashSet<HashSet<Environment>>();
        var qe = new QuadrantEvaluation(desiredClassification);
        for (int h = 0; h <= 2; ++h) {
            ProgramEnumerator pe = new ProgramEnumerator(h, h, ProgramEnumerator.B_SYMBOLS, ProgramEnumerator.E_SYMBOLS,
                    ProgramEnumerator.B_SYMBOLS);
            nextCand: while (pe.hasNext()) {
                var cond = pe.next();
                if (!qe.evaluate(cond)) {
                    // It's trivially always true/false, or otherwise deficient
                    continue;
                }
                if (qe.isPerfect()) {
                    // This one is perfect, return just it and forget everything else
                    return List.of(new Discriminator(Asts.makeBoolAstFromParse(cond), allInputs));
                }
                assert qe.isOverApproximate() || qe.isUnderApproximate();
                if (qe.isOverApproximate()) {
                    for (var oe : List.copyOf(overApproximationErrors)) {
                        if (qe.positiveErrors().size() >= oe.size()) {
                            if (qe.positiveErrors().containsAll(oe)) {
                                // Already have one that's at least as good as this
                                continue nextCand;
                            }
                        } else {
                            if (oe.containsAll(qe.positiveErrors())) {
                                // This is strictly better than the one we already have, replace that with this
                                overApproximationErrors.remove(oe);
                            }
                        }
                    }
                    overApproximationErrors.add(new HashSet<>(qe.positiveErrors()));
                } else if (qe.isUnderApproximate()) {
                    for (var ue : underApproximationErrors) {
                        if (qe.negativeErrors().size() >= ue.size()) {
                            if (qe.negativeErrors().containsAll(ue)) {
                                // Already have one that's at least as good as this
                                continue nextCand;
                            }
                        } else {
                            if (ue.containsAll(qe.negativeErrors())) {
                                // This is strictly better than the one we already have, replace that with this
                                overApproximationErrors.remove(ue);
                            }
                        }
                    }
                    underApproximationErrors.add(new HashSet<>(qe.negativeErrors()));
                }
                discriminators.add(new Discriminator(Asts.makeBoolAstFromParse(cond), allInputs));
            }
        }

        return discriminators;
    }

    private ExprRepresentation buildDecisionTreeAstFromPartialSolutions(Set<Example> allExamples,
            Collection<PartialSolution> partialSolutions, Collection<Discriminator> discriminators)
            throws InterruptedException {
        for (var ps : partialSolutions) {
            for (var ex : allExamples) {
                if (ps.application().included().contains(ex.input())) {
                    assert ps.evalExpr(ex.input()) == ex.output();
                } else if (ps.application().excluded().contains(ex.input())) {
                    assert ps.evalExpr(ex.input()) != ex.output();
                }
            }
        }
        McmcDecisionTreeOptimizer decisionTreeOptimizer = new McmcDecisionTreeOptimizer(rng.nextSubsequence(),
                partialSolutions.size(), partialSolutions, discriminators, allExamples);
        var res = decisionTreeOptimizer.optimize(10000000);
        if (!res.bestIsValid()) {
            logger.log(Level.WARNING, "Unable to produce valid decision tree");
            return null;
        }
        return res.bestX().reifyAsDecisionTree();
    }

    private ExprNode synthesizeAst(List<Example> examples) throws InterruptedException {
        var allExamples = Set.copyOf(examples);
        var allInputs = Set.of(examples.stream().map(ex -> ex.input()).toArray(Environment[]::new));

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
        var discriminators = generateDiscriminators(allInputs, partialSolutions);
        if (discriminators.size() == 0) {
            return null;
        }
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
