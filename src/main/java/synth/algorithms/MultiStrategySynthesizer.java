package synth.algorithms;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import synth.core.*;

public class MultiStrategySynthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(MultiStrategySynthesizer.class.getName());

    LinkedBlockingDeque<ProgramCandidate> candidateFifo;

    private static class ProgramCandidate {
        private Strategy source;
        private Program program;

        public Strategy source() {
            return source;
        }

        public Program program() {
            return program;
        }

        public ProgramCandidate(Strategy source, Program program) {
            this.source = source;
            this.program = program;
        }

    }

    private class Strategy {
        private String name;
        private Synthesizer synthesizer;
        private LinkedBlockingDeque<ProgramCandidate> fifo;
        private Thread thread;

        public String name() {
            return name;
        }

        public Strategy(String name, Synthesizer synthesizer) {
            this.name = name;
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
                        fifo.offer(new ProgramCandidate(Strategy.this, program));
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
    private int hardWaitMs = 30000;

    List<Strategy> strategies = List.of(
            new Strategy("MCMC", new Mcmc1Synthesizer(293874)),
            new Strategy("Voltron", new VoltronSynthesizer()),
            new Strategy("Enum", new DFSEnum2Synthesizer()));

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
            ProgramCandidate nextCand = null;
            try {
                nextCand = candidateFifo.poll(nextWaitMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return null;
            }

            if (nextCand != null) {
                logger.log(Level.INFO, "Candidate received from {0}, size {1}",
                        new Object[] { nextCand.source().name(), sizeCost(nextCand.program().getRoot()) });
                candidates.add(nextCand.program());
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
