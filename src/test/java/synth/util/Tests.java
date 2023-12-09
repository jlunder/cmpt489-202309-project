package synth.util;

import java.util.*;
import java.util.function.Consumer;

import org.junit.Assert;

import synth.algorithms.representation.*;
import synth.core.*;
import synth.dsl.*;

public class Tests {
    private static Symbol[] terminals = Arrays.stream(Symbol.values()).filter(s -> s.isTerminalProduction())
            .toArray(Symbol[]::new);

    public static Symbol[] makeRandomProgram(Random rng, int length) {
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

    public static Random makeRng(long seed) {
        var rng = new Random();
        if (seed < 0) {
            seed = Math.abs(rng.nextLong());
        }
        System.out.println("RNG seed: " + seed);
        rng.setSeed(seed);
        return rng;
    }

    public static void fuzzWithEnvs(Random rng, int count, Consumer<Environment> test) {
        for (int n = 0; n < count; ++n) {
            var env = new Environment(
                    rng.nextInt(2 * n + 2) - n - 1,
                    rng.nextInt(2 * n + 2) - n - 1,
                    rng.nextInt(2 * n + 2) - n - 1);
            test.accept(env);
        }
    }

    public static void assertProgramSatisfiesExamples(Program testProgram, Example[] testExamples) {
        for (var e : testExamples) {
            Assert.assertEquals(e.output(), Semantics.evaluate(testProgram, e.input()));
        }
    }

    public static void assertParseTreeSatisfiesExamples(ParseNode testProgram, Example[] testExamples) {
        for (var e : testExamples) {
            Assert.assertEquals(e.output(), Semantics.evaluate(testProgram, e.input()));
        }
    }

    public static void assertExprPostOrderSatisfiesExamples(Symbol[] testProgram, Example[] testExamples) {
        for (var e : testExamples) {
            Assert.assertEquals(e.output(), Semantics.evaluateExprPostOrder(testProgram, e.input()));
        }
    }

    public static void assertBoolPostOrderSatisfiesExamples(Symbol[] testProgram,
            Example[] testExamplesIncluded, Example[] testExamplesExcluded) {
        for (var e : testExamplesIncluded) {
            Assert.assertTrue(Semantics.evaluateBoolPostOrder(testProgram, e.input()));
        }
        for (var e : testExamplesExcluded) {
            Assert.assertFalse(Semantics.evaluateBoolPostOrder(testProgram, e.input()));
        }
    }

    public static void assertExprRepresentationSatisfiesExamples(ExprRepresentation testProgram,
            Example[] testExamples) {
        for (var e : testExamples) {
            Assert.assertEquals(e.output(), testProgram.evalExpr(e.input()));
        }
    }

    public static void assertBoolRepresentationSatisfiesExamples(BoolRepresentation testProgram,
            Example[] testExamplesIncluded, Example[] testExamplesExcluded) {
        for (var e : testExamplesIncluded) {
            Assert.assertTrue(testProgram.evalBool(e.input()));
        }
        for (var e : testExamplesExcluded) {
            Assert.assertFalse(testProgram.evalBool(e.input()));
        }
    }

}
