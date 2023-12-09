package synth.algorithms.ast;

import java.util.*;

import org.junit.*;

import synth.core.*;
import synth.dsl.*;
import synth.util.Tests;

public class AstFuzzTests {
    private static int programLength = 100;

    @Test
    public void testExprParseTreeAstEquivalence() {
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

    @Test
    public void testBoolParseTreeAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromBoolPostOrder(postOrder);
            BoolNode ast = Asts.makeBoolAstFromParse(parseNode);
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env),
                            ast.evalBool(env)));
        }
    }

    @Test
    public void testOptimizeExprAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromExprPostOrder(postOrder);
            ExprNode ast = Asts.makeExprAstFromParse(parseNode);
            ExprNode optimizedAst = Asts.optimizeExprAst(ast);
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            optimizedAst.evalExpr(env)));
        }
    }

    @Test
    public void testOptimizeBoolAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromBoolPostOrder(postOrder);
            BoolNode ast = Asts.makeBoolAstFromParse(parseNode);
            BoolNode optimizedAst = Asts.optimizeBoolAst(ast);
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env),
                            optimizedAst.evalBool(env)));
        }
    }

    @Test
    public void testReifyExprAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromExprPostOrder(postOrder);
            ExprNode ast = Asts.makeExprAstFromParse(parseNode);
            ParseNode reified = ast.reify();
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            Semantics.evaluate(reified, env)));
        }
    }

    @Test
    public void testReifyBoolAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromBoolPostOrder(postOrder);
            BoolNode ast = Asts.makeBoolAstFromParse(parseNode);
            ParseNode reified = new ParseNode(Symbol.Ite,
                    List.of(ast.reify(), new ParseNode(Symbol.Const1), new ParseNode(Symbol.Const2)));
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env) ? 1 : 2,
                            Semantics.evaluate(reified, env)));
        }
    }

}
