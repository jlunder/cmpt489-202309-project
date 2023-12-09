package synth.algorithms.lia;

import org.junit.*;

import synth.algorithms.ast.*;
import synth.core.*;
import synth.dsl.*;
import synth.util.Tests;

public class LinearSolutionFuzzTests {
    private static int programLength = 100;

    @Test
    public void testLinearSolutionAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromExprPostOrder(postOrder);
            ExprNode ast = Asts.makeExprAstFromParse(parseNode);
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            ast.evalExpr(env)));
        }
    }
}
