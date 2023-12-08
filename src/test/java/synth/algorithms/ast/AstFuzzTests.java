package synth.algorithms.ast;

import java.util.*;
import java.util.function.Consumer;

import org.junit.*;

import synth.core.*;
import synth.dsl.*;

public class AstFuzzTests {
    private static Symbol[] terminals = Arrays.stream(Symbol.values()).filter(s -> s.isTerminalProduction())
            .toArray(Symbol[]::new);

    private static int programLength = 100;

    private static Symbol[] makeRandomProgram(Random rng, int length) {
        Symbol[] program = new Symbol[length];
        for (int i = 0; i < length; ++i) {
            if (rng.nextInt(8) == 0) {
                program[i] = null;
            } else {
                program[i] = terminals[rng.nextInt(terminals.length)];
            }
        }
        return program;
    }

    private static Random makeRng(long seed) {
        var rng = new Random();
        if (seed < 0) {
            seed = Math.abs(rng.nextLong());
        }
        System.out.println("RNG seed: " + seed);
        rng.setSeed(seed);
        return rng;
    }

    private static void fuzzTestEnvs(Random rng, int count, Consumer<Environment> test) {
        for (int n = 0; n < count; ++n) {
            var env = new Environment(
                    rng.nextInt(2 * n + 2) - n - 1,
                    rng.nextInt(2 * n + 2) - n - 1,
                    rng.nextInt(2 * n + 2) - n - 1);
            test.accept(env);
        }
    }

    @Test
    public void testExprParseTreeAstEquivalence() {
        var rng = makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeExprParseTreeFromPostOrder(postOrder);
            ExprNode ast = Asts.makeExprAstFromParse(parseNode);
            fuzzTestEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            ast.evalExpr(env)));
        }
    }

    @Test
    public void testBoolParseTreeAstEquivalence() {
        var rng = makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeBoolParseTreeFromPostOrder(postOrder);
            BoolNode ast = Asts.makeBoolAstFromParse(parseNode);
            fuzzTestEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env),
                            ast.evalBool(env)));
        }
    }

    @Test
    public void testOptimizeExprAstEquivalence() {
        var rng = makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeExprParseTreeFromPostOrder(postOrder);
            ExprNode ast = Asts.makeExprAstFromParse(parseNode);
            ExprNode optimizedAst = Asts.optimizeExprAst(ast);
            fuzzTestEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            optimizedAst.evalExpr(env)));
        }
    }

    @Test
    public void testOptimizeBoolAstEquivalence() {
        var rng = makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeBoolParseTreeFromPostOrder(postOrder);
            BoolNode ast = Asts.makeBoolAstFromParse(parseNode);
            BoolNode optimizedAst = Asts.optimizeBoolAst(ast);
            fuzzTestEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env),
                            optimizedAst.evalBool(env)));
        }
    }

    @Test
    public void testExprAstParseTreeEquivalence() {
        var rng = makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeExprParseTreeFromPostOrder(postOrder);
            ExprNode ast = Asts.makeExprAstFromParse(parseNode);
            ParseNode reified = ast.reify();
            fuzzTestEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            Semantics.evaluate(reified, env)));
        }
    }

    @Test
    public void testBoolAstParseTreeEquivalence() {
        var rng = makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeBoolParseTreeFromPostOrder(postOrder);
            BoolNode ast = Asts.makeBoolAstFromParse(parseNode);
            ParseNode reified = new ParseNode(Symbol.Ite,
                    List.of(ast.reify(), new ParseNode(Symbol.Const1), new ParseNode(Symbol.Const2)));
            fuzzTestEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env) ? 1 : 2,
                            Semantics.evaluate(reified, env)));
        }
    }

}
