package synth.algorithms;

import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class BFSEnum2Synthesizer extends SynthesizerBase {
    private static final int ASSUME_MIN_PRODUCTIONS = 100000;
    private static final int MAX_PRODUCTIONS = 20000000;

    private static class SymbolProductions {
        final ArrayList<ParseNode> productions = new ArrayList<>(ASSUME_MIN_PRODUCTIONS);
        final ArrayList<ParseNode> newProductions = new ArrayList<>(ASSUME_MIN_PRODUCTIONS);
        final Symbol[] noArgs;
        final Symbol[] reqArgs;
        int lastGenStart = 0;

        SymbolProductions(Symbol returnSymbol) {
            this.noArgs = Grammar.getProductionOperators(returnSymbol).stream()
                    .filter(s -> s.isTerminalProduction() && !s.requiresArguments()).toArray(Symbol[]::new);
            this.reqArgs = Grammar.getProductionOperators(returnSymbol).stream()
                    .filter(s -> s.isTerminalProduction() && s.requiresArguments()).toArray(Symbol[]::new);
        }

        boolean produceOneGeneration(int maxNewProductions) {
            for (var s : reqArgs) {
                var argumentSymbols = Grammar.getOperatorArguments(s);
                var tempChildren = new ParseNode[argumentSymbols.size()];
                // k specifies which argument should *only* use trees from the last generation
                // -- this ensures we only generate fresh trees, otherwise we will duplicate all
                // of the previous generation in addition to creating new ones
                for (int k = 0; k < argumentSymbols.size(); ++k) {
                    if (!produceParseTrees(s, tempChildren, argumentSymbols, 0, k, newProductions, maxNewProductions)) {
                        return false;
                    }
                }
            }
            return true;
        }

        void nextGeneration() {
            lastGenStart = productions.size();
            productions.addAll(newProductions);
            newProductions.clear();
        }

        boolean produceParseTrees(Symbol s, ParseNode[] tempChildren, List<Symbol> argumentSymbols, int i, int k,
                List<ParseNode> newProds, int maxNewProductions) {
            if (newProds.size() >= maxNewProductions) {
                return false;
            }
            if (i == 0 && argumentSymbols.size() == 0) {
                newProds.add(ParseNode.make(s, List.of(tempChildren)));
                return true;
            }
            var argSP = (argumentSymbols.get(i) == Symbol.E) ? eProductions : bProductions;
            int n = argSP.productions.size();
            for (int j = (i == k ? argSP.lastGenStart : 0); j < n; ++j) {
                var pJ = argSP.productions.get(j);
                tempChildren[i] = pJ;
                if (i + 1 < tempChildren.length) {
                    if (!produceParseTrees(s, tempChildren, argumentSymbols, i + 1, k, newProds, maxNewProductions)) {
                        return false;
                    }
                } else {
                    if (newProds.size() >= maxNewProductions) {
                        return false;
                    }
                    newProds.add(ParseNode.make(s, List.of(tempChildren)));
                }
            }
            return true;
        }
    }

    private static SymbolProductions eProductions = new SymbolProductions(Symbol.E);
    private static SymbolProductions bProductions = new SymbolProductions(Symbol.B);

    static {
        for (var s : eProductions.noArgs) {
            eProductions.productions.add(ParseNode.make(s, ParseNode.NO_CHILDREN));
        }
    }

    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        // Since the language doesn't change, we can reuse programs we already
        // enumerated -- this method is designed to just run with whatever is in
        // eProductions and bProductions before even trying to synthesize more.

        int n = 0;
        boolean capped = false;

        do {
            while (n < eProductions.productions.size()) {
                var candidate = eProductions.productions.get(n++);
                if (validate(examples, candidate)) {
                    return new Program(candidate);
                }
            }
            for (var p : List.of(eProductions, bProductions)) {
                int productionsSoFar = eProductions.productions.size() + eProductions.newProductions.size()
                        + bProductions.productions.size() + bProductions.newProductions.size();
                capped = !p.produceOneGeneration(MAX_PRODUCTIONS - productionsSoFar);
                if (capped) {
                    System.out.println("warning: enumeration capped at " + MAX_PRODUCTIONS + " productions");
                }
            }
            for (var p : List.of(eProductions, bProductions)) {
                p.nextGeneration();
            }
            // While we are still producing new programs..
        } while (n < eProductions.productions.size());

        // enumeration stopped?
        return null;
    }

}
