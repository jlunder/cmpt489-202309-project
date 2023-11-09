package synth.algorithms;

import synth.cfg.*;
import synth.core.ASTNode;
import synth.core.Example;
import synth.core.ISynthesizer;
import synth.core.PreorderInterpreter;
import synth.core.Program;

import java.util.*;
import java.util.function.*;

public class DFSEnum1Synthesizer implements ISynthesizer {
    private CFG cfg;

    private static class ASTBuilder {
        private record State(int size) {
        }

        private Production[] preorder = new Production[1000];
        private int size = 0;

        public void add(Production p) {
            if (size + 1 >= preorder.length) {
                var newPreorder = new Production[preorder.length * 2];
                System.arraycopy(preorder, 0, newPreorder, 0, preorder.length);
                preorder = newPreorder;
            }
            preorder[size++] = p;
        }

        public State mark() {
            return new State(size);
        }

        public void reset(State state) {
            size = state.size();
        }

        public Iterator<Production> preorder() {
            return Arrays.stream(preorder, 0, size).iterator();
        }

        public ASTNode build() {
            // System.out.print("Building AST: [ ");
            // for (int i = 0; i < size; ++i) {
            // System.out.print(preorder[i] + ", ");
            // }
            // System.out.println("]");
            return buildFromPreorder(preorder());
        }

        private static ASTNode buildFromPreorder(Iterator<Production> preorder) {
            var p = preorder.next();
            var n = p.getArgumentSymbols().size();
            var children = new ArrayList<ASTNode>(n);
            for (int i = 0; i < n; ++i) {
                children.add(buildFromPreorder(preorder));
            }
            return new ASTNode(p.getOperator(), children);
        }

        public int computeHeight() {
            return computeHeightFromPreorder(preorder());
        }

        private static int computeHeightFromPreorder(Iterator<Production> preorder) {
            var p = preorder.next();
            var n = p.getArgumentSymbols().size();
            int height = 0;
            for (int i = 0; i < n; ++i) {
                height = Math.max(height, 1 + computeHeightFromPreorder(preorder));
            }
            return height;
        }
    }

    private boolean enumerateProductions(int maxHeight, Symbol symbol, ASTBuilder builder,
            Function<ASTBuilder, Boolean> progressConsumer) {
        assert maxHeight >= 0;
        for (var p : cfg.getProductions(symbol)) {
            var n = p.getArgumentSymbols().size();
            assert n >= 0;
            if (n != 0 && maxHeight == 0) {
                // We need a leaf and this production yields a branch
                continue;
            }
            if (p.getOperator().getName().equals("Ite") && maxHeight == 1) {
                // "Ite" will always have a branch beneath it
                continue;
            }
            var oldState = builder.mark();
            try {
                if (n == 0) {
                    builder.add(p);
                    if (progressConsumer.apply(builder)) {
                        return true;
                    }
                } else if (maxHeight > 0) {
                    // Add symbol to AST
                    builder.add(p);
                    // Add parameters
                    if (enumerateParameters(maxHeight - 1, p.getArgumentSymbols(), 0, builder, progressConsumer)) {
                        return true;
                    }
                }
            } finally {
                // Revert AST
                builder.reset(oldState);
            }
        }

        return false;
    }

    private boolean enumerateParameters(int maxHeight, List<Symbol> argumentSymbols,
            int argumentIndex,
            ASTBuilder builder,
            Function<ASTBuilder, Boolean> progressConsumer) {
        if (argumentIndex >= argumentSymbols.size()) {
            return progressConsumer.apply(builder);
        }
        var symbol = argumentSymbols.get(argumentIndex);
        return enumerateProductions(maxHeight, (Symbol) symbol, builder,
                builder2 -> enumerateParameters(maxHeight, argumentSymbols, argumentIndex + 1, builder2,
                        progressConsumer));
    }

    private record Validation(PreorderInterpreter interpreter, int expectedOutput) {
    }

    /**
     * Synthesize a program f(x, y, z) based on a context-free grammar and examples
     *
     * @param cfg      the context-free grammar
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(CFG cfg, List<Example> examples) {
        this.cfg = cfg;

        List<Validation> validations = examples.stream()
                .map(e -> new Validation(new PreorderInterpreter(e.getInput()), e.getOutput()))
                .toList();

        ArrayList<Program> results = new ArrayList<>();
        int maxHeight = 5;

        for (int targetHeight = 0; targetHeight <= maxHeight && results.isEmpty(); ++targetHeight) {
            var captureTargetHeight = targetHeight;
            enumerateProductions(targetHeight, cfg.getStartSymbol(), new ASTBuilder(), (builder) -> {
                if (builder.computeHeight() != captureTargetHeight) {
                    return false;
                }
                // var candidate = new Program(builder.build());
                // System.out.println("trying: " + candidate);
                if (validate(validations, builder)) {
                    results.add(new Program(builder.build()));
                    return true;
                }
                return false;
            });
        }

        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    private boolean validate(List<Validation> validations, ASTBuilder program) {
        // Run the program in each interpreter env representing a particular example,
        // and check whether the output is as expected
        for (Validation v : validations) {
            if (v.interpreter().evalExpr(program.preorder()) != v.expectedOutput()) {
                // This example produces incorrect output
                return false;
            }
        }
        // No examples failed, we have a winner!
        return true;
    }

}
