package synth.algorithms;

import synth.algorithms.mcmc.McmcOptimizer;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class Mcmc1Synthesizer extends SynthesizerBase {

    private static class McmcProgramOptimizer extends McmcOptimizer<Symbol[]> {
        private static final Symbol[] BIASED_SYMBOLS = new Symbol[128];

        static {
            int i = 0;
            for (int j = 0; j < 4; ++j) {
                BIASED_SYMBOLS[i++] = Symbol.Const1;
                BIASED_SYMBOLS[i++] = Symbol.Const1;
                BIASED_SYMBOLS[i++] = Symbol.Const1;
                BIASED_SYMBOLS[i++] = Symbol.Const2;

                BIASED_SYMBOLS[i++] = Symbol.Const3;
                BIASED_SYMBOLS[i++] = Symbol.VarX;
                BIASED_SYMBOLS[i++] = Symbol.VarY;
                BIASED_SYMBOLS[i++] = Symbol.VarZ;

                BIASED_SYMBOLS[i++] = Symbol.Add;
                BIASED_SYMBOLS[i++] = Symbol.Add;
                BIASED_SYMBOLS[i++] = Symbol.Multiply;
                BIASED_SYMBOLS[i++] = Symbol.Multiply;

                BIASED_SYMBOLS[i++] = Symbol.Lt;
                BIASED_SYMBOLS[i++] = Symbol.Eq;
                BIASED_SYMBOLS[i++] = Symbol.And;
                BIASED_SYMBOLS[i++] = Symbol.Or;

                BIASED_SYMBOLS[i++] = Symbol.Lt;
                BIASED_SYMBOLS[i++] = Symbol.Eq;
                BIASED_SYMBOLS[i++] = Symbol.Not;
                BIASED_SYMBOLS[i++] = Symbol.Ite;
            }
            // The rest of the symbols in the table are null, i.e. no-op
        }

        private List<Example> examples;
        private Symbol[] spare;

        public McmcProgramOptimizer(List<Example> examples) {
            super(new Xoshiro256SS(2390845));
            this.examples = examples;
        }

        @Override
        protected float computeCost(Symbol[] x) {
            int failures = 0;
            for (int i = 0; i < Math.min(50, examples.size()); ++i) {
                var e = examples.get(i);
                if (Semantics.evaluatePostOrder(x, e.input()) != e.output()) {
                    ++failures;
                }
            }
            return (float) failures;
        }

        @Override
        protected Symbol[] generateFrom(Symbol[] x) {
            Symbol[] newX = spare;
            spare = null;
            if (newX == null || newX.length != x.length) {
                newX = new Symbol[x.length];
            }
            System.arraycopy(x, 0, newX, 0, x.length);

            float f = rng().nextFloat();
            int num = (int)(1f / (0.1f + 1.9f * f)) + 1;
            for (int i = 0; i < num; ++i) {
                int index = rng().nextInt(x.length);
                int sym = rng().nextInt(BIASED_SYMBOLS.length);
                newX[index] = BIASED_SYMBOLS[sym];
            }
            return newX;
        }

        @Override
        protected void discard(Symbol[] x) {
            spare = x;
        }
    }

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        McmcProgramOptimizer optimizer = new McmcProgramOptimizer(examples);

        try {
            var result = optimizer.optimize(new Symbol[20], 0.5f, 1000000);
            if (result.reachedTargetCost()) {
                return new Program(Semantics.makeParseTreeFromPostOrder(result.bestX()));
            } else {
                System.out.println(
                        String.format("Best cost: %g after %d iterations", result.bestCost(), result.iterations()));
                return null;
            }
        } catch (InterruptedException e) {
            System.out.println(e);
            return null;
        }
    }

}
