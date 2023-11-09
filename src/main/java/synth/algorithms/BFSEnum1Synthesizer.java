package synth.algorithms;

import synth.core.*;
import synth.dsl.*;

import java.util.*;
import java.util.function.*;

public class BFSEnum1Synthesizer implements ISynthesizer {
    private static class EnumerationJob {
        // AST written out postfix order, with null for holes
        private final Symbol[] productions;

        // Locations of holes (in symbols) -- this is stored explicitly so that the
        // order will be maintained, this is important because holes need to be filled
        // in breadth-first
        private final int[] holeLocations;
        private final Symbol[] holeSymbols;

        public EnumerationJob(Symbol[] productions, int[] holeLocations, Symbol[] holeSymbols) {
            this.productions = productions;
            this.holeLocations = holeLocations;
            this.holeSymbols = holeSymbols;
        }

        /**
         * Enumerates all ASTs produced by filling in holes in this job's partial AST.
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
            for (var op : Grammar.getProductionOperators(s)) {
                var args = Grammar.getOperatorArguments(op);
                productions[i] = op;
                if (args.isEmpty() && (holeLocations.length == 1)) {
                    var program = new Program(constructAST(new ProductionsStack(productions)));
                    if (validate.apply(program)) {
                        return program;
                    }
                } else {
                    int carryoverHoles = holeLocations.length - 1;
                    int remainingHoles = carryoverHoles + args.size();

                    if (remainingHoles == 0) {
                        var program = new Program(constructAST(new ProductionsStack(productions)));
                        if (validate.apply(program)) {
                            return program;
                        }
                    } else {
                        Symbol[] newProductions = new Symbol[productions.length + args.size()];
                        System.arraycopy(productions, 0, newProductions, 0, i);
                        int j = i + args.size();
                        System.arraycopy(productions, i, newProductions, j, productions.length - i);

                        int[] remainingHoleLocations = new int[remainingHoles];
                        System.arraycopy(holeLocations, 1, remainingHoleLocations, 0, carryoverHoles);

                        Symbol[] remainingHoleSymbols = new Symbol[remainingHoles];
                        System.arraycopy(holeSymbols, 1, remainingHoleSymbols, 0, holeSymbols.length - 1);

                        for (int k = 0; k < (carryoverHoles); ++k) {
                            if (remainingHoleLocations[k] >= i) {
                                remainingHoleLocations[k] += args.size();
                            }
                        }
                        for (int k = 0; k < args.size(); ++k) {
                            --j;
                            remainingHoleLocations[carryoverHoles + k] = j;
                            remainingHoleSymbols[carryoverHoles + k] = args.get(k);
                        }

                        jobs.accept(new EnumerationJob(newProductions, remainingHoleLocations,
                                remainingHoleSymbols));
                    }
                }
            }

            // No candidates passed validation
            return null;
        }

        private static class ProductionsStack {
            private int top;
            private final Symbol[] productions;

            public ProductionsStack(Symbol[] productions) {
                this.top = productions.length;
                this.productions = productions;
            }

            Symbol pop() {
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
            var op = stack.pop();
            assert op != null;
            var argSymbols = Grammar.getOperatorArguments(op);
            List<ASTNode> children = noChildren;
            if (!argSymbols.isEmpty()) {
                children = new ArrayList<ASTNode>(argSymbols.size());
                for (int i = 0; i < argSymbols.size(); ++i) {
                    children.add(constructAST(stack));
                }
            }
            return ASTNode.make(op, children);
        }
    }

    private static final int MAX_PENDING_JOBS = 10000000;

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        var jobs = new ArrayDeque<EnumerationJob>();
        boolean aborting = false;

        for (var j = new EnumerationJob(new Symbol[] { null }, new int[] { 0 },
                new Symbol[] { Grammar.startSymbol() }); j != null; j = jobs.poll()) {
            if (jobs.size() >= MAX_PENDING_JOBS) {
                if (!aborting) {
                    System.out.println("warning: enumeration capped at " + MAX_PENDING_JOBS + " pending jobs");
                }
                aborting = true;
            }
            var program = j.enumerate(candidate -> validate(examples, candidate), aborting ? job -> {
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

    private boolean validate(List<Example> examples, Program program) {
        // Run the program in each interpreter env representing a particular example,
        // and check whether the output is as expected
        for (Example ex : examples) {
            if (Semantics.evaluate(program, ex.getInput()) != ex.getOutput()) {
                // This example produces incorrect output
                return false;
            }
        }
        // No examples failed, we have a winner!
        return true;
    }

}
