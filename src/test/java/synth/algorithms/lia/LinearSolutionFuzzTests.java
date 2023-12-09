package synth.algorithms.lia;

import java.util.*;

import org.junit.*;

import synth.core.*;
import synth.dsl.*;
import synth.util.Tests;

public class LinearSolutionFuzzTests {
    public static LinearSolution makeRandomLinearSolution(Random rng) {
        int nTerms = rng.nextInt(10) + 1;
        var coeffs = new HashMap<Term, Integer>();
        for (int i = 0; i < nTerms; ++i) {
            var t = Term.fromIndex(rng.nextInt(i + 2));
            coeffs.put(t, coeffs.getOrDefault(t, 0) + rng.nextInt(10) + 1);
        }
        return new LinearSolution(coeffs);
    }

    @Test
    public void testLinearSolutionAstEquivalence() {
        var rng = Tests.makeRng(-1);
        for (int n = 0; n < 1000; ++n) {
            var linSol = makeRandomLinearSolution(rng);
            ParseNode parseNode = linSol.reifyAsExprParse();
            Tests.fuzzWithEnvs(rng, 100,
                    (env) -> Assert.assertEquals(Semantics.evaluate(parseNode, env), linSol.evalExpr(env)));
        }
    }
}
