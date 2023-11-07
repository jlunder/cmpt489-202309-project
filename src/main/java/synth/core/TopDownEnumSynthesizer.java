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

        /**
         * Enumerates all ASTs produced by
         * 
         * @param asts function receiving finished ASTs; if the function returns true
         *             when executed, enumeration stops
         * @param jobs consumer receiving jobs for ASTs which still have holes -- the
         *             job will enumerate ASTs which fill a further hole
         * @return a new index
         */

        public Program enumerate(Function<Program, Boolean> validate, Consumer<EnumerationJob> jobs) {
            assert holeLocations.length == holeSymbols.length;
            assert holeSymbols.length > 0;
            var i = holeLocations[0];
            var s = holeSymbols[0];
            for (var p : cfg.getProductions(s)) {
                var args = p.getArgumentSymbols();
                productions[i] = p;
                if (args.isEmpty() && (holeLocations.length == 1)) {
                    var program = new Program(constructAST(new ProductionsStack(productions)));
                    if (validate.apply(program)) {
                        return program;
                    }
                } else {
                    Production[] newProductions = new Production[productions.length + args.size()];
                    System.arraycopy(productions, 0, newProductions, 0, i);
                    int j = i + args.size();
                    System.arraycopy(productions, i, newProductions, j, productions.length - i);

                    int carryoverHoles = holeLocations.length - 1;
                    int remainingHoles = carryoverHoles + args.size();

                    if (remainingHoles == 0) {
                        var program = new Program(constructAST(new ProductionsStack(productions)));
                        if (validate.apply(program)) {
                            return program;
                        }
                    } else {
                        int[] remainingHoleLocations = new int[remainingHoles];
                        System.arraycopy(holeLocations, 1, remainingHoleLocations, 0, carryoverHoles);

                        NonTerminal[] remainingHoleSymbols = new NonTerminal[remainingHoles];
                        System.arraycopy(holeSymbols, 1, remainingHoleSymbols, 0, holeSymbols.length - 1);

                        for (int k = 0; k < (carryoverHoles); ++k) {
                            if (remainingHoleLocations[k] >= i) {
                                remainingHoleLocations[k] += args.size();
                            }
                        }
                        for (int k = 0; k < args.size(); ++k) {
                            --j;
                            remainingHoleLocations[carryoverHoles + k] = j;
                            remainingHoleSymbols[carryoverHoles + k] = (NonTerminal) args.get(k);
                        }

                        jobs.accept(new EnumerationJob(cfg, newProductions, remainingHoleLocations,
                                remainingHoleSymbols));
                    }
                }
            }

            // No candidates passed validation
            return null;
        }

        private static class ProductionsStack {
            private int top;
            private final Production[] productions;

            public ProductionsStack(Production[] productions) {
                this.top = productions.length;
                this.productions = productions;
            }

            Production pop() {
                return productions[--top];
            }
        }

        private static List<ASTNode> noChildren = List.of();

        /**
         * Construct an AST from a post-order array of productions. E.g., an array
         * like { x, y, Add } becomes Add(x, y); {x, y, Add, z, Add } becomes Add(z,
         * Add(x, y)).
         */
        private ASTNode constructAST(ProductionsStack stack) {
            var p = stack.pop();
            assert p != null;
            var argSymbols = p.getArgumentSymbols();
            List<ASTNode> children = noChildren;
            if (!argSymbols.isEmpty()) {
                children = new ArrayList<ASTNode>(argSymbols.size());
                for (int i = 0; i < argSymbols.size(); ++i) {
                    children.add(constructAST(stack));
                }
            }
            return ASTNode.make(p.getOperator(), children);
        }
    }

    private record Validation(Interpreter interpreter, int expectedOutput) {
    };

    private static final int MAX_PENDING_JOBS = 10000000;

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

        var jobs = new ArrayDeque<EnumerationJob>();
        boolean aborting = false;

        for (var j = new EnumerationJob(cfg, new Production[] { null }, new int[] { 0 },
                new NonTerminal[] { cfg.getStartSymbol() }); j != null; j = jobs.poll()) {
            if (jobs.size() >= MAX_PENDING_JOBS) {
                aborting = true;
            }
            var program = j.enumerate(candidate -> validate(validations, candidate), aborting ? job -> {
                return;
            } : job -> jobs.offer(job));
            if (program != null) {
                return program;
            }
        }
        // No jobs left -- we failed to generate a program. This shouldn't happen
        // without a recursion limit..?
        return null;
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
