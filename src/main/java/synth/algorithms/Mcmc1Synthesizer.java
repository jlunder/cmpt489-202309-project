package synth.algorithms;

import synth.algorithms.mcmc.McmcProgramOptimizer;
import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class Mcmc1Synthesizer extends SynthesizerBase {

    private long seed = 2390845;
    private int maxProgramLength = 50;
    private int maxExamples = 100;
    private int maxIterations = 1000000;

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
        List<Example> limitedExamples = examples;
        if (examples.size() > maxExamples) {
            limitedExamples = examples.subList(0, maxExamples);
        }
        McmcProgramOptimizer optimizer = new McmcProgramOptimizer(seed,
                McmcProgramOptimizer.examplesCostFunction(limitedExamples), McmcProgramOptimizer.GENERAL_SYMBOLS);

        try {
            var result = optimizer.optimize(new Symbol[maxProgramLength], 0.5f, maxIterations);
            if (result.reachedTargetCost()) {
                return new Program(Semantics.makeExprParseTreeFromPostOrder(result.bestX()));
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
