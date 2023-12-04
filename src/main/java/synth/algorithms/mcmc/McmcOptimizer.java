package synth.algorithms.mcmc;

import synth.algorithms.rng.Xoshiro256SS;

public abstract class McmcOptimizer<T> {
    public static class OptimizationResult<U> {
        private boolean reachedTargetCost;
        private U bestX;
        private float bestCost;
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

        public long iterations() {
            return iterations;
        }

        public OptimizationResult(boolean reachedTargetCost, U bestX, float bestCost, long iterations) {
            this.reachedTargetCost = reachedTargetCost;
            this.bestX = bestX;
            this.bestCost = bestCost;
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

    protected abstract T generateFrom(T x);

    protected void discard(T x) {
        // In case you want to implement a resource reuse policy -- we don't promise to
        // ALWAYS discard, this is best effort basis!
    }

    protected abstract float computeCost(T x);

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

    public OptimizationResult<T> optimize(T initialX, float targetCost, long maxIterations)
            throws InterruptedException {
        T curX = initialX;
        float curCost = computeCost(curX);

        T bestX = curX;
        float bestCost = curCost;

        long i;
        for (i = 0; i < maxIterations && bestCost > targetCost; ++i) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Thread interrupted during McmcOptimizer::optimize()");
            }

            T candidateX = generateFrom(curX);
            float candidateCost = computeCost(candidateX);
            boolean accepted = (rng.nextFloat() < acceptProbability(curCost, candidateCost));
            boolean best = (candidateCost < bestCost);

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

        return new OptimizationResult<T>(bestCost <= targetCost, bestX, bestCost, i);
    }

}
