package synth.algorithms.mcmc;

import java.util.List;
import java.util.function.Function;

import synth.algorithms.classify.Classification;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;
import synth.dsl.*;

public class McmcProgramOptimizer extends McmcOptimizer<Symbol[]> {
    public static final Symbol[] GENERAL_SYMBOLS = new Symbol[32];
    public static final Symbol[] CONDITION_SYMBOLS = new Symbol[16];

    static {
        int i = 0;
        GENERAL_SYMBOLS[i++] = Symbol.Const1;
        GENERAL_SYMBOLS[i++] = Symbol.Const2;
        GENERAL_SYMBOLS[i++] = Symbol.Const3;
        GENERAL_SYMBOLS[i++] = Symbol.VarX;
        GENERAL_SYMBOLS[i++] = Symbol.VarY;
        GENERAL_SYMBOLS[i++] = Symbol.VarZ;
        GENERAL_SYMBOLS[i++] = Symbol.Add;
        GENERAL_SYMBOLS[i++] = Symbol.Multiply;
        GENERAL_SYMBOLS[i++] = Symbol.Lt;
        GENERAL_SYMBOLS[i++] = Symbol.Eq;
        GENERAL_SYMBOLS[i++] = Symbol.And;
        GENERAL_SYMBOLS[i++] = Symbol.Or;
        GENERAL_SYMBOLS[i++] = Symbol.Not;
        GENERAL_SYMBOLS[i++] = Symbol.Ite;
        // The rest of the symbols in the table are null, i.e. no-op

        i = 0;
        CONDITION_SYMBOLS[i++] = Symbol.Const1;
        CONDITION_SYMBOLS[i++] = Symbol.Const2;
        CONDITION_SYMBOLS[i++] = Symbol.Const3;
        CONDITION_SYMBOLS[i++] = Symbol.VarX;
        CONDITION_SYMBOLS[i++] = Symbol.VarY;
        CONDITION_SYMBOLS[i++] = Symbol.VarZ;
        CONDITION_SYMBOLS[i++] = Symbol.Add;
        CONDITION_SYMBOLS[i++] = Symbol.Multiply;
        CONDITION_SYMBOLS[i++] = Symbol.Lt;
        CONDITION_SYMBOLS[i++] = Symbol.Eq;
        // The rest of the symbols in the table are null, i.e. no-op
    }

    private Function<Symbol[], Float> costFunction;
    private Symbol[] symbolPool;

    private Symbol[] spare;

    public McmcProgramOptimizer(long seed, Function<Symbol[], Float> costFunction, Symbol[] symbolPool) {
        super(new Xoshiro256SS(seed));
        this.costFunction = costFunction;
        this.symbolPool = symbolPool;
    }

    public static Function<Symbol[], Float> examplesCostFunction(List<Example> examples) {
        return x -> {
            int failures = 0;
            for (int i = 0; i < examples.size(); ++i) {
                var e = examples.get(i);
                if (Semantics.evaluateExprPostOrder(x, e.input()) != e.output()) {
                    ++failures;
                }
            }
            return (float) failures;
        };
    }

    public static Function<Symbol[], Float> classificationCostFunction(Classification classification) {
        return x -> {
            int failures = 0;
            for (var e : classification.included()) {
                if (!Semantics.evaluateBoolPostOrder(x, e.input())) {
                    ++failures;
                }
            }
            for (var e : classification.excluded()) {
                if (Semantics.evaluateBoolPostOrder(x, e.input())) {
                    ++failures;
                }
            }
            return (float) failures;
        };
    }

    @Override
    protected float computeCost(Symbol[] x) {
        return costFunction.apply(x);
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
        int num = (int) (1f / (0.1f + 1.9f * f)) + 1;
        for (int i = 0; i < num; ++i) {
            int index = rng().nextInt(x.length);
            int sym = rng().nextInt(symbolPool.length);
            newX[index] = symbolPool[sym];
        }
        return newX;
    }

    @Override
    protected void discard(Symbol[] x) {
        spare = x;
    }
}
