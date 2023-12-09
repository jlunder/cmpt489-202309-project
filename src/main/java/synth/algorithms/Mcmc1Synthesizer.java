package synth.algorithms;

import synth.algorithms.mcmc.McmcProgramOptimizer;
import synth.algorithms.mcmc.McmcOptimizer.OptimizationResult;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;
import synth.core.Program;
import synth.dsl.*;

import java.util.*;
import java.util.function.Function;
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
        McmcProgramOptimizer optimizer = new McmcProgramOptimizer(rng.nextSubsequence());
        var cost = McmcProgramOptimizer.examplesCostFunction(examples);
        var generateFrom = optimizer.generateFromFunction(McmcProgramOptimizer.GENERAL_SYMBOLS);
        Function<Symbol[], Boolean> validate = (x) -> {
            for (var e : examples) {
                var programOutput = Semantics.evaluateExprPostOrder(x, e.input());
                if (programOutput != e.output()) {
                    return false;
                }
            }
            return true;
        };

        try {
            var x = optimizer.makeRandomized(maxProgramLength, McmcProgramOptimizer.GENERAL_SYMBOLS);
            OptimizationResult<Symbol[]> result = null;
            for (int i = 3; i > 0; --i) {
                result = optimizer.optimize(x, generateFrom, cost, (examples.size() >> i) / 4, someX -> true, 10000000);
                x = result.bestX();
                logger.log(Level.INFO, "Best result: {0}",
                        new Object[] { Semantics.makeParseTreeFromExprPostOrder(result.bestX()) });
            }
            result = optimizer.optimize(result.bestX(), generateFrom, cost, 10f, validate, maxIterations);
            if (result != null && !result.bestIsValid()) {
                logger.log(Level.INFO, "Best cost: {0} after {1} iterations",
                        new Object[] { result.bestCost(), result.iterations() });
                return null;
            }
            return new Program(Semantics.makeParseTreeFromExprPostOrder(result.bestX()));
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupted during synthesize()");
            return null;
        }
    }

}
