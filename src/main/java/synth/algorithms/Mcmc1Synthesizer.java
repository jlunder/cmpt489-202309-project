package synth.algorithms;

import synth.algorithms.mcmc.McmcProgramOptimizer;
import synth.algorithms.mcmc.McmcOptimizer.OptimizationResult;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;
import synth.dsl.*;

import java.util.*;
import java.util.logging.*;

public class Mcmc1Synthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(Mcmc1Synthesizer.class.getName());

    private long seed = 2390845;
    private int maxProgramLength = 40;
    private int maxIterations = 10000000;

    public Mcmc1Synthesizer() {
    }

    public Mcmc1Synthesizer(long seed) {
        this.seed = seed;
    }

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        var rng = new Xoshiro256SS(seed);
        McmcProgramOptimizer optimizer = new McmcProgramOptimizer(rng.nextSubsequence(),
                McmcProgramOptimizer.examplesCostFunction(examples),
                McmcProgramOptimizer.GENERAL_SYMBOLS);

        try {
            var x = optimizer.makeRandomized(maxProgramLength);
            OptimizationResult<Symbol[]> result = null;
            for (int i = 3; i > 0; --i) {
                McmcProgramOptimizer roughOptimizer = new McmcProgramOptimizer(rng.nextSubsequence(),
                        McmcProgramOptimizer.examplesCostFunction(examples.subList(0, examples.size() >> i)),
                        McmcProgramOptimizer.GENERAL_SYMBOLS);
                result = roughOptimizer.optimize(x, (examples.size() >> i) / 4, null, 10000000);
                x = result.bestX();
                logger.log(Level.INFO, "Best result: {0}",
                        new Object[] { Semantics.makeExprParseTreeFromPostOrder(result.bestX()) });
            }
            result = optimizer.optimize(result.bestX(), 10f, expr -> {
                for (var e : examples) {
                    if (Semantics.evaluateExprPostOrder(expr, e.input()) != e.output()) {
                        return false;
                    }
                }
                return true;
            }, maxIterations);
            if (result != null && !result.reachedTargetCost()) {
                logger.log(Level.INFO, "Best cost: {0} after {1} iterations",
                        new Object[] { result.bestCost(), result.iterations() });
                return null;
            }
            return new Program(Semantics.makeExprParseTreeFromPostOrder(result.bestX()));
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupted during synthesize()");
            return null;
        }
    }

}
