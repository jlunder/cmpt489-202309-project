package synth.algorithms.enumeration;

import java.util.*;

import synth.core.*;
import synth.dsl.*;

public class ProgramEnumerator implements Iterator<ParseNode> {
    public static final Symbol[] E_SYMBOLS = Grammar.getProductionOperators(Symbol.E).stream()
            .filter(s -> s.isTerminalProduction()).toArray(Symbol[]::new);
    public static final Symbol[] B_SYMBOLS = Grammar.getProductionOperators(Symbol.B).stream()
            .filter(s -> s.isTerminalProduction()).toArray(Symbol[]::new);

    private static NodeGenerator[] EMPTY_GENS = new NodeGenerator[0];
    private static ParseNode[] EMPTY_CHILDREN = new ParseNode[0];

    private class NodeGenerator {
        int minHeight;
        int maxHeight;
        Symbol[] iteratingSymbols;
        int curSymbol;
        NodeGenerator[] argGenerators;
        ParseNode[] children;

        public NodeGenerator(int minHeight, int maxHeight, Symbol[] permitted) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.iteratingSymbols = permitted;
            reset();
        }

        public boolean hasNext() {
            return curSymbol < iteratingSymbols.length;
        }

        public ParseNode next() {
            if (curSymbol >= iteratingSymbols.length) {
                throw new IllegalStateException("Iterating past end of available nodes");
            }
            ParseNode node = new ParseNode(iteratingSymbols[curSymbol], List.of(children));
            if (!advanceArgs()) {
                advanceSymbol();
            }
            return node;
        }

        public void reset() {
            curSymbol = -1;
            advanceSymbol();
        }

        private void advanceSymbol() {
            ++curSymbol;
            while (curSymbol < iteratingSymbols.length) {
                var s = iteratingSymbols[curSymbol];
                if (!s.requiresArguments()) {
                    // We wouldn't be here if maxHeight < 0, but what about minHeight?
                    if (minHeight == 0) {
                        argGenerators = EMPTY_GENS;
                        children = EMPTY_CHILDREN;
                        return;
                    }
                } else if (maxHeight > 0) {
                    // Requires children: try to find children that fit within the size limits
                    var args = s.operatorArguments();
                    var n = args.size();
                    if (argGenerators == null || argGenerators.length != n) {
                        argGenerators = new NodeGenerator[n];
                        children = new ParseNode[n];
                    }
                    var complete = true;
                    for (int i = 0; i < n; ++i) {
                        argGenerators[i] = new NodeGenerator(minHeight > 0 ? minHeight - 1 : 0, maxHeight - 1,
                                (args.get(i) == Symbol.B) ? permittedBool : permittedExpr);
                        if (!argGenerators[i].hasNext()) {
                            complete = false;
                            break;
                        }
                        children[i] = argGenerators[i].next();
                    }
                    if (complete) {
                        // Done! All children successfully instantiated
                        return;
                    }
                }
                ++curSymbol;
            }
        }

        private boolean advanceArgs() {
            var args = iteratingSymbols[curSymbol].operatorArguments();
            var n = args.size();
            for (int i = 0; i < n; ++i) {
                if (argGenerators[i].hasNext()) {
                    children[i] = argGenerators[i].next();
                    return true;
                } else {
                    argGenerators[i].reset();
                    assert argGenerators[i].hasNext();
                    children[i] = argGenerators[i].next();
                    // And cascade to the next higher as well
                }
            }
            // If the overflow cascades right off the end, we're done with this production
            return false;
        }
    }

    Symbol[] permittedExpr;
    Symbol[] permittedBool;

    NodeGenerator root;

    public ProgramEnumerator(int minHeight, int maxHeight, Symbol[] seeds, Symbol[] permittedExpr, Symbol[] permittedBool) {
        this.permittedExpr = permittedExpr;
        this.permittedBool = permittedBool;

        root = new NodeGenerator(minHeight, maxHeight, seeds);
    }

    @Override
    public boolean hasNext() {
        return root.hasNext();
    }

    @Override
    public ParseNode next() {
        return root.next();
    }
}
