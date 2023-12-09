package synth.dsl;

import java.util.*;

import org.junit.*;

import synth.core.*;
import synth.util.Tests;

public class SemanticsFuzzTests {
    private static int programLength = 100;

    @Test
    public void testExprParseTreePostOrderEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromExprPostOrder(postOrder);
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            Semantics.evaluate(parseNode, env)));
        }
    }

    @Test
    public void testBoolParseTreePostOrderEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = new ParseNode(Symbol.Ite, List.of(Semantics.makeParseTreeFromBoolPostOrder(postOrder),
                    new ParseNode(Symbol.Const1), new ParseNode(Symbol.Const2)));
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env) ? 1 : 2,
                            Semantics.evaluate(parseNode, env)));
        }
    }

    @Test
    public void testExprParseTreePostOrderHeightEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = Semantics.makeParseTreeFromExprPostOrder(postOrder);
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateExprPostOrder(postOrder, env),
                            Semantics.evaluate(parseNode, env)));
        }
    }

    @Test
    public void testBoolParseTreePostOrderHeightEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            Symbol[] postOrder = Tests.makeRandomProgram(rng, programLength);
            ParseNode parseNode = new ParseNode(Symbol.Ite, List.of(Semantics.makeParseTreeFromBoolPostOrder(postOrder),
                    new ParseNode(Symbol.Const1), new ParseNode(Symbol.Const2)));
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluateBoolPostOrder(postOrder, env) ? 1 : 2,
                            Semantics.evaluate(parseNode, env)));
        }
    }

}
