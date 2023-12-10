package synth.algorithms.mcmc;

import java.util.*;
import java.util.function.Function;

import synth.algorithms.classify.*;
import synth.algorithms.representation.*;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Environment;
import synth.core.Example;
import synth.util.Bits;

public class McmcDecisionTreeOptimizer extends McmcOptimizer<McmcDecisionTreeOptimizer.FlatDecisionTree> {
    public class FlatDecisionTree {
        private Discriminator[] discriminators;
        private int[] jumpTable;
        private PartialSolution[] solutions;

        public Discriminator[] discriminators() {
            return discriminators;
        }

        public int[] jumpTable() {
            return jumpTable;
        }

        public PartialSolution[] solutions() {
            return solutions;
        }

        public int treeSize() {
            return treeSize;
        }

        public FlatDecisionTree() {
            discriminators = new Discriminator[treeSize];
            jumpTable = new int[treeSize];
            solutions = new PartialSolution[treeSize];
        }

        public void randomize() {
            var r = rng();
            for (int i = 0; i < treeSize(); ++i) {
                discriminators[i] = discriminatorPool[r.nextInt(discriminatorPool.length)];
                jumpTable[i] = (r.nextBoolean() ? -1 : 1) * r.nextInt(treeSize());
                solutions[i] = solutionPool[r.nextInt(solutionPool.length)];
            }
        }

        public void copyFrom(FlatDecisionTree other) {
            assert discriminators.length == other.discriminators.length && solutions.length == other.solutions.length;
            System.arraycopy(other.discriminators, 0, discriminators, 0, discriminators.length);
            System.arraycopy(other.jumpTable, 0, jumpTable, 0, jumpTable.length);
            System.arraycopy(other.solutions, 0, solutions, 0, solutions.length);
        }

        public void mutate() {
            var r = rng();
            switch (r.nextInt(6)) {
                case 0:
                    discriminators[r.nextInt(treeSize())] = discriminatorPool[r.nextInt(discriminatorPool.length)];
                    break;
                case 1: {
                    int i = r.nextInt(treeSize()), j = r.nextInt(treeSize());
                    var tmp = discriminators[i];
                    discriminators[i] = discriminators[j];
                    discriminators[j] = tmp;
                    break;
                }
                case 2:
                    solutions[r.nextInt(treeSize())] = solutionPool[r.nextInt(solutionPool.length)];
                    break;
                case 3: {
                    int i = r.nextInt(treeSize()), j = r.nextInt(treeSize());
                    var tmp = solutions[i];
                    solutions[i] = solutions[j];
                    solutions[j] = tmp;
                    break;
                }
                case 4:
                    jumpTable[r.nextInt(treeSize())] = r.nextInt(treeSize()) * 2 - treeSize();
                    break;
                case 5: {
                    int i = r.nextInt(treeSize());
                    jumpTable[i] = ~jumpTable[i];
                    break;
                }
                default:
                    break;
            }
        }

        public float cost(Example e) {
            int cost = 0;
            int index = 0;
            int i;
            for (i = 0; i < treeSize(); ++i) {
                var jump = jumpTable[index];
                if (jump == index || jump < 0) {
                    break;
                } else if (discriminators[index].classification().included().contains(e.input())) {
                    index = jump;
                } else {
                    index = (jump + 1) % treeSize();
                }
            }
            if (!solutions[index].application().included().contains(e.input())) {
                cost += misclassifyCost;
            }
            cost += i;
            if (i == treeSize()) {
                cost += dawdleCost;
            }
            return (float) cost;
        }

        public ExprRepresentation reifyAsDecisionTree() {
            // The behaviour of this function has to match the cost function precisely -- I
            // have minor regrets about writing the two in such different styles (recursive
            // vs. imperative) but here we are. The least obvious consequence of them
            // differing will be failure to converge on a working decision tree.
            return reifyAsDecisionTreeRecurse(0, treeSize());
        }

        private ExprRepresentation reifyAsDecisionTreeRecurse(int index, int maxSteps) {
            var jump = jumpTable[index];
            if (maxSteps == 0 || jump == index || jump < 0) {
                return solutions[index];
            } else {
                return new DecisionTree(discriminators[index], reifyAsDecisionTreeRecurse(jump, maxSteps - 1),
                        reifyAsDecisionTreeRecurse((jump + 1) % treeSize(), maxSteps - 1));
            }
        }
    }

    private int treeSize;

    private PartialSolution[] solutionPool;
    private Discriminator[] discriminatorPool;
    private Collection<Example> examples;

    private int misclassifyCost;
    private int dawdleCost;

    private FlatDecisionTree spare;

    public FlatDecisionTree makeRandomized() {
        var x = new FlatDecisionTree();
        x.randomize();
        return x;
    }

    public McmcDecisionTreeOptimizer(Xoshiro256SS rng, int treeSize, Collection<PartialSolution> solutionPool,
            Collection<Discriminator> discriminatorPool, Collection<Example> examples) {
        super(rng);

        this.treeSize = Bits.nextPower2(treeSize);

        this.solutionPool = solutionPool.toArray(PartialSolution[]::new);
        this.discriminatorPool = discriminatorPool.toArray(Discriminator[]::new);
        this.examples = List.copyOf(examples);

        this.misclassifyCost = 10 * examples.size();
        this.dawdleCost = 10;
    }

    @Override
    protected void discard(FlatDecisionTree x) {
        spare = x;
    }

    public OptimizationResult<FlatDecisionTree> optimize(int maxIterations) throws InterruptedException {
        var inputs = Set.of(examples.stream().map(ex -> ex.input()).toArray(Environment[]::new));

        Function<FlatDecisionTree, Boolean> validate = (dt) -> {
            var d = dt.reifyAsDecisionTree();
            if (d instanceof PartialSolution) {
                // Degenerate case
                return ((PartialSolution) d).application().included().containsAll(inputs);
            } else {
                // Classify examples using the decision tree, and check they ended up in a
                // compatible solution
                for (var input : inputs) {
                    var ps = (PartialSolution) ((DecisionTree) d).classify(input);
                    if (!ps.application().included().contains(input)) {
                        return false;
                    }
                }
                return true;
            }
        };

        return super.optimize(makeRandomized(), this::generateFrom, this::computeCost,
                examples.size() * treeSize / 2 + 0.5f, validate, maxIterations);
    }

    protected float computeCost(FlatDecisionTree x) {
        float cost = 0f;
        for (var e : examples) {
            cost += x.cost(e);
        }
        return cost;
    }

    protected FlatDecisionTree generateFrom(FlatDecisionTree x) {
        var newX = spare;
        spare = null;
        if (newX == null || newX.treeSize() != treeSize) {
            newX = new FlatDecisionTree();
        }

        newX.copyFrom(x);
        newX.mutate();

        return newX;
    }
}
