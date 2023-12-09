package synth.algorithms.mcmc;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import synth.algorithms.classify.Classification;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;
import synth.dsl.*;

public class McmcProgramOptimizer extends McmcOptimizer<Symbol[]> {
    public static final Symbol[] GENERAL_SYMBOLS = new Symbol[64];
    public static final Symbol[] CONDITION_SYMBOLS = new Symbol[32];

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

    private Symbol[] spare;

    public McmcProgramOptimizer(Xoshiro256SS rng) {
        super(rng);
    }

    @Override
    protected void discard(Symbol[] x) {
        spare = x;
    }

    public Symbol[] makeRandomized(int length, Symbol[] symbolPool) {
        var x = new Symbol[length];
        for (int i = 0; i < length; ++i) {
            x[i] = symbolPool[rng().nextInt(symbolPool.length)];
        }
        return x;
    }

    public static Function<Symbol[], Float> examplesCostFunction(Collection<Example> examples) {
        return x -> {
            int cost = 0;
            for (var e : examples) {
                int eval = Semantics.evaluateExprPostOrder(x, e.input());
                if (eval > e.output()) {
                    cost += 20;
                } else {
                    cost += Math.min(20, e.output() - eval);
                }
            }
            return (float) cost;
        };
    }

    public static Function<Symbol[], Float> examplesCostFunction(List<Example> examples, Xoshiro256SS rng,
            int sampleCount) {
        return x -> {
            int cost = 0;
            for (int i = 0; i < sampleCount; ++i) {
                var e = examples.get(rng.nextInt(examples.size()));
                int eval = Semantics.evaluateExprPostOrder(x, e.input());
                if (eval > e.output()) {
                    cost += 40;
                } else if (eval < e.output()) {
                    cost += 20 + Math.min(20, e.output() - eval);
                }
            }
            return (float) cost * examples.size() / sampleCount;
        };
    }

    public static Function<Symbol[], Float> exprSizeCostDecoratorFunction(Function<Symbol[], Float> otherCost,
            float sizeBias) {
        return (x) -> {
            return otherCost.apply(x) + sizeBias * Semantics.measureExprPostOrderSize(x);
        };
    }

    public static Function<Symbol[], Float> boolSizeCostDecoratorFunction(Function<Symbol[], Float> otherCost,
            float sizeBias) {
        return (x) -> {
            return otherCost.apply(x) + sizeBias * Semantics.measureBoolPostOrderSize(x);
        };
    }

    public static Function<Symbol[], Float> confusionCostFunction(Classification classification,
            float falsePositiveBias, float falseNegativeBias) {
        var inSize = classification.included().size();
        var exSize = classification.excluded().size();
        if (inSize == 0 && exSize == 0) {
            throw new IllegalArgumentException("classification is empty");
        }
        // Optimize degenerate cases, just 'cause
        if (inSize == 0) {
            return inclusionsCostFunction(classification.included());
        } else if (exSize == 0) {
            return exclusionsCostFunction(classification.excluded());
        }
        var infn = inclusionsCostFunction(classification.included());
        var exfn = exclusionsCostFunction(classification.excluded());

        // Scale the cost of errors so that really lopsided classifications don't
        // generate trivial classifiers that always classify things the same way.
        
        // Inclusion cost is incurred when something that was supposed to be included
        // wasn't, i.e. false negative.
        float inScale = falseNegativeBias * (float) Math.max(inSize, exSize) / inSize;
        
        // Exclusion cost is incurred when something that was supposed to be excluded
        // wasn't, i.e. false positive.
        float exScale = falsePositiveBias * (float) Math.max(inSize, exSize) / exSize;
        
        return x -> infn.apply(x) * inScale + exfn.apply(x) * exScale;
    }

    public static Function<Symbol[], Float> inclusionsCostFunction(Collection<Example> included) {
        return x -> {
            int failures = 0;
            for (var e : included) {
                if (!Semantics.evaluateBoolPostOrder(x, e.input())) {
                    ++failures;
                }
            }
            return (float) failures;
        };
    }

    public static Function<Symbol[], Float> inclusionsCostFunction(List<Example> included, Xoshiro256SS rng,
            int sampleCount) {
        return x -> {
            int failures = 0;
            for (int i = 0; i < sampleCount; ++i) {
                var e = included.get(rng.nextInt(included.size()));
                if (!Semantics.evaluateBoolPostOrder(x, e.input())) {
                    ++failures;
                }
            }
            return (float) failures;
        };
    }

    public static Function<Symbol[], Float> exclusionsCostFunction(Collection<Example> excluded) {
        return x -> {
            int failures = 0;
            for (var e : excluded) {
                if (Semantics.evaluateBoolPostOrder(x, e.input())) {
                    ++failures;
                }
            }
            return (float) failures;
        };
    }

    public static Function<Symbol[], Float> exclusionsCostFunction(List<Example> excluded, Xoshiro256SS rng,
            int sampleCount) {
        return x -> {
            int failures = 0;
            for (int i = 0; i < sampleCount; ++i) {
                var e = excluded.get(rng.nextInt(excluded.size()));
                if (Semantics.evaluateBoolPostOrder(x, e.input())) {
                    ++failures;
                }
            }
            return (float) failures;
        };
    }

    private static final Map<Symbol, Symbol> ROTATE_LEFT_MAP = Map.of(
            Symbol.Const1, Symbol.Const2,
            Symbol.Const2, Symbol.Const3,
            Symbol.Const3, Symbol.VarX,
            Symbol.VarX, Symbol.VarY,
            Symbol.VarY, Symbol.VarZ,
            Symbol.VarZ, Symbol.Const1);

    private static final Map<Symbol, Symbol> ROTATE_RIGHT_MAP = Map.of(
            Symbol.Const1, Symbol.VarZ,
            Symbol.Const2, Symbol.Const1,
            Symbol.Const3, Symbol.Const2,
            Symbol.VarX, Symbol.Const3,
            Symbol.VarY, Symbol.VarX,
            Symbol.VarZ, Symbol.VarY);

    private static final Map<Symbol, Symbol> SWAP_MAP = Map.of(
            Symbol.Add, Symbol.Multiply,
            Symbol.Multiply, Symbol.Add,
            Symbol.Lt, Symbol.Eq,
            Symbol.Eq, Symbol.Lt,
            Symbol.And, Symbol.Or,
            Symbol.Or, Symbol.And);

    public Function<Symbol[], Symbol[]> generateFromFunction(Symbol[] symbolPool) {
        final List<Consumer<Symbol[]>> mutators = List.of(
                (Symbol[] x) -> {
                    int a = rng().nextInt(x.length), b = rng().nextInt(x.length);
                    if (a != b) {
                        int i = Math.min(a, b), j = Math.max(a, b);
                        var tmp = rng().nextBoolean() ? x[j] : null;
                        System.arraycopy(x, i, x, i + 1, j - i - 1);
                        x[i] = tmp;
                    }
                },
                (Symbol[] x) -> {
                    int a = rng().nextInt(x.length), b = rng().nextInt(x.length);
                    if (a != b) {
                        int i = Math.min(a, b), j = Math.max(a, b);
                        var tmp = rng().nextBoolean() ? x[j] : null;
                        System.arraycopy(x, i + 1, x, i, j - i - 1);
                        x[j] = tmp;
                    }
                },
                (Symbol[] x) -> {
                    int i = rng().nextInt(x.length);
                    var xi = x[i];
                    if (xi == null) {
                    } else if (xi == Symbol.Not) {
                        xi = null;
                    } else if (xi == Symbol.Ite) {
                        // same
                    } else if (SWAP_MAP.containsKey(xi)) {
                        xi = SWAP_MAP.get(xi);
                    } else if (rng().nextBoolean()) {
                        xi = ROTATE_LEFT_MAP.get(xi);
                    } else {
                        xi = ROTATE_RIGHT_MAP.get(xi);
                    }
                    x[i] = xi;
                },
                (Symbol[] x) -> {
                    int i = rng().nextInt(x.length);
                    int sym = rng().nextInt(symbolPool.length);
                    x[i] = symbolPool[sym];
                });

        return (x) -> {
            Symbol[] newX = spare;
            spare = null;
            if (newX == null || newX.length != x.length) {
                newX = new Symbol[x.length];
            }
            System.arraycopy(x, 0, newX, 0, x.length);
            mutators.get(rng().nextInt(mutators.size())).accept(newX);
            return newX;
        };
    }
}
