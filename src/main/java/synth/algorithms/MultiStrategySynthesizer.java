package synth.algorithms;

import java.util.*;
import java.util.concurrent.*;

import synth.core.*;

public class MultiStrategySynthesizer extends SynthesizerBase {
    LinkedBlockingDeque<Program> candidateFifo;

    private class Strategy {
        private Synthesizer synthesizer;
        private LinkedBlockingDeque<Program> fifo;
        private Thread thread;

        public Strategy(Synthesizer synthesizer) {
            this.synthesizer = synthesizer;
        }

        public void start(List<Example> examples) {
            assert (this.thread == null);
            fifo = candidateFifo;
            this.thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    var program = synthesizer.synthesize(examples);
                    if (program != null) {
                        fifo.offer(program);
                    }
                }
            });
            this.thread.start();
        }

        public boolean checkFinished() {
            return thread != null && thread.isAlive();
        }

        public void stop() {
            try {
                thread.interrupt();
                thread.join();
            } catch (InterruptedException e) {
                // Not sure why this would happen, but it's probably fine anyway
            }
            thread = null;
        }
    }

    // If we have softCandidateCount candidates before softWaitMs, we interrupt the
    // remaining synthesizers early.
    private int softCandidateCount = 1;
    private int softWaitMs = 5000;

    // If after softWaitMs we still don't have any candidates, we will wait up to
    // hardWaitMs for at least one candidate. After that we interrupt threads and
    // abort.
    private int hardWaitMs = 10000;

    List<Strategy> strategies = List.of(
            new Strategy(new Mcmc1Synthesizer(293874)),
            new Strategy(new Mcmc1Synthesizer(950862)),
            new Strategy(new Mcmc1Synthesizer(342981)),
            new Strategy(new BFSEnum2Synthesizer()));

    @Override
    public Program synthesize(List<Example> examples) {
        // Avoid a potential hole where the join from the Strategy's stop() gets
        // interrupted. Can't really happen I know, but whatever, might as well make a
        // new one of these fresh whenever we synthesize(), it doesn't hurt
        candidateFifo = new LinkedBlockingDeque<>();

        ArrayList<Program> candidates = new ArrayList<>();
        HashSet<Strategy> runningStrategies = new HashSet<>();

        // Kick off our search...
        for (var strategy : strategies) {
            strategy.start(examples);
            runningStrategies.add(strategy);
        }

        // And wait for the results to roll in!
        long startNs = System.nanoTime();
        long nextWaitMs = softWaitMs;
        while (nextWaitMs > 0 && runningStrategies.size() > 0) {
            Program nextCand = null;
            try {
                nextCand = candidateFifo.poll(nextWaitMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return null;
            }

            if (nextCand != null) {
                candidates.add(nextCand);
            }
            if (candidates.size() >= softCandidateCount) {
                break;
            }

            long nowNs = System.nanoTime();
            long msSinceStart = (nowNs - startNs) / 1000000;
            nextWaitMs = ((candidates.size() > 0) ? softWaitMs : hardWaitMs) - msSinceStart;

            for (var strategy : List.copyOf(runningStrategies)) {
                if (strategy.checkFinished()) {
                    strategy.stop();
                    runningStrategies.remove(strategy);
                }
            }
        }

        // Wait for the system to come to a complete stop
        for (var strategy : runningStrategies) {
            strategy.stop();
        }

        if (candidates.size() > 0) {
            int bestCost = Integer.MAX_VALUE;
            Program bestCand = null;
            for (var cand : candidates) {
                int candCost = sizeCost(cand.getRoot());
                if (candCost < bestCost) {
                    bestCand = cand;
                }
            }
            return bestCand;
        } else {
            // No candidates :(
            return null;
        }
    }

    private static int sizeCost(ParseNode node) {
        int size = 1;
        for (var child : node.getChildren()) {
            size += sizeCost(child);
        }
        return size;
    }
}
