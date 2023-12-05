package synth.algorithms;

import synth.algorithms.mcmc.McmcProgramOptimizer;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;
import synth.dsl.*;

import java.util.*;
import java.util.logging.*;

public class Mcmc1Synthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(Mcmc1Synthesizer.class.getName());

    private long seed = 2390845;
    private int maxProgramLength = 50;
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
        McmcProgramOptimizer optimizer = new McmcProgramOptimizer(rng,
                McmcProgramOptimizer.examplesCostFunction(examples, rng.nextSubsequence(), 100),
                McmcProgramOptimizer.GENERAL_SYMBOLS);

        try {
            var result = optimizer.optimize(new Symbol[maxProgramLength], 0.5f, maxIterations);
            if (result.reachedTargetCost()) {
                return new Program(Semantics.makeExprParseTreeFromPostOrder(result.bestX()));
            } else {
                logger.log(Level.INFO, "Best cost: {0} after {1} iterations",
                        new Object[] { result.bestCost(), result.iterations() });
                return null;
            }
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupted during synthesize()");
            return null;
        }
    }

}
