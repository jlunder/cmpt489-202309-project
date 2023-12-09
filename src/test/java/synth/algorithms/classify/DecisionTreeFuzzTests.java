package synth.algorithms.classify;

import java.util.*;

import org.junit.*;

import synth.algorithms.ast.*;
import synth.algorithms.lia.*;
import synth.algorithms.representation.*;
import synth.core.*;
import synth.dsl.*;
import synth.util.Tests;

public class DecisionTreeFuzzTests {
    private static BoolRepresentation makeRandomCond(Random rng) {
        Symbol[] postOrder = Tests.makeRandomProgram(rng, 50);
        var parseNode = Semantics.makeParseTreeFromBoolPostOrder(postOrder);
        return Asts.optimizeBoolAst(Asts.makeBoolAstFromParse(parseNode));
    }

    private static ExprRepresentation makeRandomExpr(Random rng) {
        return LinearSolutionFuzzTests.makeRandomLinearSolution(rng).reifyAsExprAst();
    }

    public static DecisionTree makeRandomDecisionTree(Random rng, int depth, Collection<Environment> inputs) {
        var cond = makeRandomCond(rng);
        var thenBranch = (depth > 0 && rng.nextBoolean()) ? makeRandomDecisionTree(rng, depth - 1, inputs) : makeRandomExpr(rng);
        var elseBranch = (depth > 0 && rng.nextBoolean()) ? makeRandomDecisionTree(rng, depth - 1, inputs) : makeRandomExpr(rng);
        return new DecisionTree(new Discriminator(cond, inputs), thenBranch, elseBranch);
    }

    @Test
    public void testDecisionTreeEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            var dt = makeRandomDecisionTree(rng, 4, List.of());
            var ast = dt.reifyAsExprAst();

            Tests.fuzzWithEnvs(rng, 100, (env) -> Assert.assertEquals(dt.evalExpr(env), ast.evalExpr(env)));
        }
    }
}
