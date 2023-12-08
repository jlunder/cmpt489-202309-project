package synth.algorithms;

import synth.core.Example;
import synth.core.ParseNode;
import synth.core.Program;
import synth.dsl.*;

import java.util.*;
import java.util.logging.*;

public class BFSEnum2Synthesizer extends SynthesizerBase {
    private static Logger logger = Logger.getLogger(BFSEnum2Synthesizer.class.getName());

    private static final int ASSUME_MIN_PRODUCTIONS = 100000;
    private static final int MAX_PRODUCTIONS = 20000000;

    private static class SymbolProductions {
        final ArrayList<ParseNode> productions = new ArrayList<>(ASSUME_MIN_PRODUCTIONS);
        final ArrayList<ParseNode> newProductions = new ArrayList<>(ASSUME_MIN_PRODUCTIONS);
        final Symbol[] noArgs;
        final Symbol[] reqArgs;
        int lastGenStart = 0;
        int genNo = 0;

        SymbolProductions(Symbol returnSymbol) {
            this.noArgs = Grammar.getProductionOperators(returnSymbol).stream()
                    .filter(s -> s.isTerminalProduction() && !s.requiresArguments()).toArray(Symbol[]::new);
            this.reqArgs = Grammar.getProductionOperators(returnSymbol).stream()
                    .filter(s -> s.isTerminalProduction() && s.requiresArguments()).toArray(Symbol[]::new);
        }

        boolean produceOneGeneration(int maxNewProductions) throws InterruptedException {
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
            ++genNo;
            logger.log(Level.INFO, "Generation {0} ({1}): {2} productions ({3} + {4} new) from {5} seeds",
                    new Object[] { genNo, reqArgs[0].returnSymbol().token(), productions.size() + newProductions.size(),
                            productions.size(), newProductions.size(), noArgs.length + reqArgs.length });
            lastGenStart = productions.size();
            productions.addAll(newProductions);
            newProductions.clear();
        }

        boolean produceParseTrees(Symbol s, ParseNode[] tempChildren, List<Symbol> argumentSymbols, int i, int k,
                List<ParseNode> newProds, int maxNewProductions) throws InterruptedException {
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
                if (Thread.interrupted()) {
                    throw new InterruptedException(
                            "Thread interrupted during BFSEnum2Synthesizer.SymbolProductions::produceParseTrees()");
                }
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

    private static final int MAX_GENERATIONS = 2;
    private int gen = 0;
    private boolean capped = false;

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

        try {
            do {
                while (n < eProductions.productions.size()) {
                    var candidate = eProductions.productions.get(n++);
                    if (validate(examples, candidate)) {
                        return new Program(candidate);
                    }
                }
                if (!capped && gen < MAX_GENERATIONS) {
                    for (var p : List.of(eProductions, bProductions)) {
                        int productionsSoFar = eProductions.productions.size() + eProductions.newProductions.size()
                                + bProductions.productions.size() + bProductions.newProductions.size();
                        capped = !p.produceOneGeneration(MAX_PRODUCTIONS - productionsSoFar);
                        if (capped) {
                            logger.log(Level.WARNING, "Enumeration capped at {0} productions",
                                    new Object[] { MAX_PRODUCTIONS });
                            break;
                        }
                    }
                    for (var p : List.of(eProductions, bProductions)) {
                        p.nextGeneration();
                    }
                    ++gen;
                    if (gen == MAX_GENERATIONS) {
                        logger.log(Level.INFO, "Stopping production at {0} generations",
                                new Object[] { MAX_GENERATIONS });
                    }
                }
                // While we are still producing new programs..
            } while (n < eProductions.productions.size());
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupted during synthesize()");
            return null;
        }

        // enumeration stopped?
        return null;
    }

}
