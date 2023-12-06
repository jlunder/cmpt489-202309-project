package synth.algorithms.mcmc;

import java.util.*;

import synth.algorithms.classify.*;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.*;

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

        public void generate() {
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
                    jumpTable[r.nextInt(treeSize())] = (r.nextBoolean() ? -1 : 1) * r.nextInt(treeSize());
                    break;
                case 5: {
                    int i = r.nextInt(treeSize());
                    jumpTable[i] = -jumpTable[i];
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
                var d = discriminators[index];
                var c = d.classification();
                var inc = c.included();
                var selected = inc.contains(e);
                if (selected) {
                    if (jump > 0) {
                        ++index;
                        if (index >= treeSize()) {
                            index -= treeSize();
                        }
                    } else {
                        break;
                    }
                } else {
                    index = Math.abs(jump);
                }
            }
            if (!solutions[index].application().included().contains(e)) {
                cost += 100;
            }
            cost += i * i;
            return (float) cost;
        }
    }

    private int treeSize;
    private PartialSolution[] solutionPool;
    private Discriminator[] discriminatorPool;
    private Collection<Example> examples;

    private FlatDecisionTree spare;

    public McmcDecisionTreeOptimizer(Xoshiro256SS rng, int treeSize, PartialSolution[] solutionPool,
            Collection<Example> examples) {
        super(rng);
        assert (treeSize & (treeSize - 1)) == 0;
        this.treeSize = treeSize;
        this.solutionPool = solutionPool;
        var discriminators = new HashSet<Discriminator>();
        for (var sol : solutionPool) {
            discriminators.addAll(sol.positiveDiscriminators());
            discriminators.addAll(sol.negativeDiscriminators());
        }
        this.discriminatorPool = discriminators.toArray(Discriminator[]::new);
        this.examples = examples;
    }

    @Override
    public float computeCost(FlatDecisionTree x) {
        float cost = 0f;
        for (var e : examples) {
            cost += x.cost(e);
        }
        return cost;
    }

    @Override
    protected FlatDecisionTree generateFrom(FlatDecisionTree x) {
        var newX = spare;
        spare = null;
        if (newX == null || newX.treeSize() != treeSize) {
            newX = new FlatDecisionTree();
        }

        newX.copyFrom(x);
        newX.generate();

        return newX;
    }

    @Override
    protected void discard(FlatDecisionTree x) {
        spare = x;
    }
}
