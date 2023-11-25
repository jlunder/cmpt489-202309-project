package synth.algorithms;

import synth.core.*;
import synth.dsl.*;

import java.util.*;
import java.util.function.*;

public class DFSEnum1Synthesizer implements ISynthesizer {
    private static class ParseTreeBuilder {
        private record State(int size) {
        }

        private Symbol[] preorder = new Symbol[1000];
        private int size = 0;

        public void add(Symbol s) {
            if (size + 1 >= preorder.length) {
                var newPreorder = new Symbol[preorder.length * 2];
                System.arraycopy(preorder, 0, newPreorder, 0, preorder.length);
                preorder = newPreorder;
            }
            preorder[size++] = s;
        }

        public State mark() {
            return new State(size);
        }

        public void reset(State state) {
            size = state.size();
        }

        public Iterator<Symbol> preorder() {
            return Arrays.stream(preorder, 0, size).iterator();
        }

        public ParseNode build() {
            // System.out.print("Building parse tree: [ ");
            // for (int i = 0; i < size; ++i) {
            // System.out.print(preorder[i] + ", ");
            // }
            // System.out.println("]");
            return buildFromPreorder(preorder());
        }

        private static ParseNode buildFromPreorder(Iterator<Symbol> preorder) {
            var s = preorder.next();
            var n = Grammar.getOperatorArguments(s).size();
            var children = new ArrayList<ParseNode>(n);
            for (int i = 0; i < n; ++i) {
                children.add(buildFromPreorder(preorder));
            }
            return new ParseNode(s, children);
        }

        public int computeHeight() {
            return computeHeightFromPreorder(preorder());
        }

        private static int computeHeightFromPreorder(Iterator<Symbol> preorder) {
            var s = preorder.next();
            var n = Grammar.getOperatorArguments(s).size();
            int height = 0;
            for (int i = 0; i < n; ++i) {
                height = Math.max(height, 1 + computeHeightFromPreorder(preorder));
            }
            return height;
        }
    }

    private boolean enumerateProductions(int maxHeight, Symbol symbol, ParseTreeBuilder builder,
            Function<ParseTreeBuilder, Boolean> progressConsumer) {
        assert maxHeight >= 0;
        for (var op : Grammar.getProductionOperators(symbol)) {
            var n = Grammar.getOperatorArguments(op).size();
            assert n >= 0;
            if (n != 0 && maxHeight == 0) {
                // We need a leaf and this production yields a branch
                continue;
            }
            if (op == Symbol.Ite && maxHeight == 1) {
                // "Ite" will always have a branch beneath it
                continue;
            }
            var oldState = builder.mark();
            try {
                if (n == 0) {
                    builder.add(op);
                    if (progressConsumer.apply(builder)) {
                        return true;
                    }
                } else if (maxHeight > 0) {
                    // Add symbol to parse tree
                    builder.add(op);
                    // Add parameters
                    if (enumerateParameters(maxHeight - 1, Grammar.getOperatorArguments(op), 0, builder, progressConsumer)) {
                        return true;
                    }
                }
            } finally {
                // Revert tree
                builder.reset(oldState);
            }
        }

        return false;
    }

    private boolean enumerateParameters(int maxHeight, List<Symbol> argumentSymbols,
            int argumentIndex,
            ParseTreeBuilder builder,
            Function<ParseTreeBuilder, Boolean> progressConsumer) {
        if (argumentIndex >= argumentSymbols.size()) {
            return progressConsumer.apply(builder);
        }
        var symbol = argumentSymbols.get(argumentIndex);
        return enumerateProductions(maxHeight, (Symbol) symbol, builder,
                builder2 -> enumerateParameters(maxHeight, argumentSymbols, argumentIndex + 1, builder2,
                        progressConsumer));
    }

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        ArrayList<Program> results = new ArrayList<>();
        int maxHeight = 5;

        for (int targetHeight = 0; targetHeight <= maxHeight && results.isEmpty(); ++targetHeight) {
            var captureTargetHeight = targetHeight;
            enumerateProductions(targetHeight, Grammar.startSymbol(), new ParseTreeBuilder(), (builder) -> {
                if (builder.computeHeight() != captureTargetHeight) {
                    return false;
                }
                // var candidate = new Program(builder.build());
                // System.out.println("trying: " + candidate);
                if (validate(examples, builder)) {
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

    private boolean validate(List<Example> examples, ParseTreeBuilder program) {
        // Run the program in each interpreter env representing a particular example,
        // and check whether the output is as expected
        for (Example ex : examples) {
            if (Semantics.evaluate(program.preorder(), ex.getInput()) != ex.getOutput()) {
                // This example produces incorrect output
                return false;
            }
        }
        // No examples failed, we have a winner!
        return true;
    }

}
