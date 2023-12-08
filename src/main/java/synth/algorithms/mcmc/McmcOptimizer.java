package synth.algorithms.mcmc;

import java.util.function.Function;
import java.util.logging.*;

import synth.algorithms.rng.Xoshiro256SS;

public abstract class McmcOptimizer<T> {
    private static Logger logger = Logger.getLogger(McmcOptimizer.class.getName());

    public static class OptimizationResult<U> {
        private boolean reachedTargetCost;
        private U bestX;
        private float bestCost;
        private boolean bestIsValid;
        private long iterations;

        public boolean reachedTargetCost() {
            return reachedTargetCost;
        }

        public U bestX() {
            return bestX;
        }

        public float bestCost() {
            return bestCost;
        }

        public boolean bestIsValid() {
            return bestIsValid;
        }

        public long iterations() {
            return iterations;
        }

        public OptimizationResult(boolean reachedTargetCost, U bestX, float bestCost, boolean bestIsValid,
                long iterations) {
            this.reachedTargetCost = reachedTargetCost;
            this.bestX = bestX;
            this.bestCost = bestCost;
            this.bestIsValid = bestIsValid;
            this.iterations = iterations;
        }
    }

    private Xoshiro256SS rng;

    protected Xoshiro256SS rng() {
        return rng;
    }

    public McmcOptimizer(Xoshiro256SS rng) {
        this.rng = rng;
    }

    protected void discard(T x) {
        // In case you want to implement a resource reuse policy -- we don't promise to
        // ALWAYS discard, this is best effort basis!
    }

    protected float acceptProbability(float curCost, float candidateCost) {
        // if checks probably make this faster by skipping exp, also if the cost value
        // reaches 0 then exp gets squirrelly -- this is only mathematically significant
        // in that it's a special case to handle, so I'm comfortable that this
        // formulation bypasses exp when candidateCost is potentially <= 0.
        if (candidateCost < curCost) {
            return 1;
        }
        if (curCost <= 0f) {
            return 0f;
        }
        final float beta = 1f;
        return (float) Math.exp(-beta * candidateCost / curCost);
    }

    public OptimizationResult<T> optimize(T initialX, Function<T, T> generateFrom, Function<T, Float> computeCost,
            float targetCost, Function<T, Boolean> validate, long maxIterations) throws InterruptedException {
        logger.log(Level.INFO, "Begin MCMC optimize of {0}, target cost {1}, max iterations {2}",
                new Object[] { initialX.getClass().getSimpleName(), targetCost, maxIterations });
        T curX = initialX;
        float curCost = computeCost.apply(curX);

        T bestX = curX;
        float bestCost = curCost;
        boolean bestIsValid = false;

        final long giga = 1000000000;
        long i;
        long startNs = System.nanoTime();
        long lastNs = startNs;
        for (i = 0; i < maxIterations && bestCost > targetCost; ++i) {
            long nowNs = System.nanoTime();
            if (nowNs - lastNs > giga) {
                lastNs = nowNs - (nowNs - startNs) % giga;
                logger.log(Level.INFO, "MCMC heartbeat: cost {0} (best {1}), {2} iterations ({3}/s)",
                        new Object[] { curCost, bestCost, i, i * giga / (nowNs - startNs) });
            }

            if (Thread.interrupted()) {
                throw new InterruptedException("Thread interrupted during McmcOptimizer::optimize()");
            }

            T candidateX = generateFrom.apply(curX);
            float candidateCost = computeCost.apply(candidateX);
            boolean accepted = (rng.nextFloat() < acceptProbability(curCost, candidateCost));
            boolean best = (candidateCost < bestCost);

            // If this is a better solution, or it's just as good and we don't already have
            // a validated solution, check this solution
            if (validate != null && (best || (candidateCost == bestCost && !bestIsValid))) {
                if (validate.apply(candidateX)) {
                    // The solution validated! Keep it
                    best = true;
                    bestIsValid = true;
                    break;
                }
                // If we didn't break above, we would have to check whether any new best that
                // doesn't validate is overwriting one that does...
            }

            if (accepted && (best || curX != bestX)) {
                discard(curX);
            } else if (!accepted && !best) {
                discard(candidateX);
            }

            if (accepted) {
                curX = candidateX;
                curCost = candidateCost;
            }
            if (best) {
                bestX = candidateX;
                bestCost = candidateCost;
            }
        }

        return new OptimizationResult<T>(bestCost <= targetCost, bestX, bestCost, bestIsValid, i);
    }

}
