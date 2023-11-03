package synth.core;

import synth.cfg.*;

import java.util.*;
import java.util.function.*;

public class TopDownEnumSynthesizer implements ISynthesizer {
    private class EnumerationJob {
        private final CFG cfg;

        // AST written out postfix order, with null for holes
        private final Production[] productions;

        // Locations of holes (in symbols) -- this is stored explicitly so that the
        // order will be maintained, this is important because holes need to be filled
        // in breadth-first
        private final int[] holeLocations;
        private final NonTerminal[] holeSymbols;

        public EnumerationJob(CFG cfg, Production[] productions, int[] holeLocations, NonTerminal[] holeSymbols) {
            this.cfg = cfg;
            this.productions = productions;
            this.holeLocations = holeLocations;
            this.holeSymbols = holeSymbols;
        }

        public void enumerate(Consumer<ASTNode> asts, Consumer<EnumerationJob> jobs) {
            assert holeLocations.length == holeSymbols.length;
            assert holeSymbols.length > 0;
            var i = holeLocations[0];
            var s = holeSymbols[0];
            for (var p : cfg.getProductions(s)) {
                var args = p.getArgumentSymbols();
                productions[i] = p;
                if (args.isEmpty() && (holeLocations.length == 1)) {
                    constructAST(productions.length, asts);
                } else {
                    enumerateCombinations(args, combination -> {
                        // Insert these productions along with their associated holes at i:
                        int addedHoleCount = 0;
                        for (var p2 : combination) {
                            addedHoleCount += p2.getArgumentSymbols().size();
                        }
                        Production[] newProductions = new Production[productions.length + addedHoleCount + args.size()];
                        System.arraycopy(productions, 0, newProductions, 0, i);
                        int j = i + addedHoleCount + args.size();
                        System.arraycopy(productions, i, newProductions, j, productions.length - i);

                        int remainingHoles = holeSymbols.length - 1 + addedHoleCount;
                        if (remainingHoles == 0) {
                            for (var p2 : combination) {
                                --j;
                                newProductions[j] = p2;
                            }
                            constructAST(newProductions.length, asts);
                        } else {
                            int[] remainingHoleLocations = new int[remainingHoles];
                            System.arraycopy(holeLocations, 1, remainingHoleLocations, 0, holeLocations.length - 1);

                            NonTerminal[] remainingHoleSymbols = new NonTerminal[remainingHoles];
                            System.arraycopy(holeSymbols, 1, remainingHoleSymbols, 0, holeSymbols.length - 1);

                            int k = holeLocations.length - 1;
                            for (var p2 : combination) {
                                --j;
                                newProductions[j] = p2;
                                for (var p2a : p2.getArgumentSymbols()) {
                                    assert j > i;
                                    --j;
                                    // newProductions[j] = null; // Implicitly already null
                                    assert k < remainingHoleLocations.length;
                                    remainingHoleLocations[k] = j;
                                    remainingHoleSymbols[k] = (NonTerminal) p2a;
                                    ++k;
                                }
                            }
                            assert j == i;
                            jobs.accept(new EnumerationJob(cfg, newProductions, remainingHoleLocations,
                                    remainingHoleSymbols));
                        }
                    });
                }
            }
        }

        private int constructAST(int i, Consumer<ASTNode> asts) {
            assert i > 0 && i <= productions.length;
            var j = i - 1;
            var p = productions[j];
            assert p != null;
            var argSymbols = p.getArgumentSymbols();
            if (!argSymbols.isEmpty()) {
                var children = new ArrayList<ASTNode>(argSymbols.size());
                for (int k = 0; k < argSymbols.size(); ++k) {
                    j = constructAST(j, ast -> children.add(ast));
                }
                asts.accept(ASTNode.make(p.getOperator(), children));
            } else {
                asts.accept(ASTNode.make(p.getOperator(), List.of()));
            }
            return j;
        }

        private void enumerateCombinations(List<Symbol> symbols, Consumer<Production[]> consumer) {
            enumerateCombinationsRecurse(symbols, new Production[symbols.size()], 0, consumer);
        }

        private void enumerateCombinationsRecurse(List<Symbol> symbols, Production[] partial, int index,
                Consumer<Production[]> consumer) {
            if (index + 1 < partial.length) {
                for (var p : cfg.getProductions((NonTerminal) symbols.get(index))) {
                    partial[index] = p;
                    enumerateCombinationsRecurse(symbols, partial, index + 1, consumer);
                }
            } else {
                for (var p : cfg.getProductions((NonTerminal) symbols.get(index))) {
                    partial[index] = p;
                    consumer.accept(partial);
                }
            }
        }
    }

    private record Validation(Interpreter interpreter, int expectedOutput) {
    };

    /**
     * Synthesize a program f(x, y, z) based on a context-free grammar and examples
     *
     * @param cfg      the context-free grammar
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(CFG cfg, List<Example> examples) {
        List<Validation> validations = examples.stream()
                .map(e -> new Validation(new Interpreter(e.getInput()), e.getOutput()))
                .toList();

        var programs = new ArrayDeque<Program>();
        var jobs = new ArrayDeque<EnumerationJob>();

        jobs.offer(new EnumerationJob(cfg, new Production[] { null }, new int[] { 0 },
                new NonTerminal[] { cfg.getStartSymbol() }));

        while (true) {
            var j = jobs.poll();
            if (j == null) {
                // No jobs left -- we failed to generate a program. This shouldn't happen
                // without a recursion limit..?
                return null;
            }

            j.enumerate(ast -> programs.offer(new Program(ast)), job -> jobs.offer(job));
            // Validate fully-formed candidate programs first
            for (var candidate : programs) {
                if (validate(validations, candidate)) {
                    // Successful candidate!
                    return candidate;
                }
            }
        }
    }

    private boolean validate(List<Validation> validations, Program program) {
        // Run the program in each interpreter env representing a particular example,
        // and check whether the output is as expected
        for (Validation v : validations) {
            if (v.interpreter().evalExpr(program.getRoot()) != v.expectedOutput()) {
                // This example produces incorrect output
                return false;
            }
        }
        // No examples failed, we have a winner!
        return true;
    }

}
