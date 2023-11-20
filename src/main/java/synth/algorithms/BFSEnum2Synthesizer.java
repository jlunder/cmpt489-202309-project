package synth.algorithms;

import synth.core.*;
import synth.dsl.*;

import java.util.*;

public class BFSEnum2Synthesizer implements ISynthesizer {
    private static final int ASSUME_MIN_PRODUCTIONS = 100000;
    private static final int MAX_PRODUCTIONS = 20000000;

    private static class SymbolProductions {
        final ArrayList<ASTNode> productions = new ArrayList<>(ASSUME_MIN_PRODUCTIONS);
        final ArrayList<ASTNode> newProductions = new ArrayList<>(ASSUME_MIN_PRODUCTIONS);
        final Symbol[] terminals;
        final Symbol[] nonTerminals;
        int lastGenStart = 0;

        SymbolProductions(Symbol returnSymbol) {
            this.terminals = Grammar.getProductionOperators(returnSymbol).stream()
                    .filter(s -> s.isTerminal()).toArray(Symbol[]::new);
            this.nonTerminals = Grammar.getProductionOperators(returnSymbol).stream()
                    .filter(s -> s.isNonTerminal()).toArray(Symbol[]::new);
        }

        boolean produceOneGeneration(int maxNewProductions) {
            for (var s : nonTerminals) {
                var argumentSymbols = Grammar.getOperatorArguments(s);
                var tempChildren = new ASTNode[argumentSymbols.size()];
                // k specifies which argument should *only* use ASTs from the last generation --
                // this ensures we only generate fresh ASTs, otherwise we will duplicate all of
                // the previous generation in addition to creating new ones
                for (int k = 0; k < argumentSymbols.size(); ++k) {
                    if (!produceASTs(s, tempChildren, argumentSymbols, 0, k, newProductions, maxNewProductions)) {
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

        boolean produceASTs(Symbol s, ASTNode[] tempChildren, List<Symbol> argumentSymbols, int i, int k,
                List<ASTNode> newProds, int maxNewProductions) {
            if (newProds.size() >= maxNewProductions) {
                return false;
            }
            if (i == 0 && argumentSymbols.size() == 0) {
                newProds.add(ASTNode.make(s, List.of(tempChildren)));
                return true;
            }
            var argSP = (argumentSymbols.get(i) == Symbol.E) ? eProductions : bProductions;
            int n = argSP.productions.size();
            for (int j = (i == k ? argSP.lastGenStart : 0); j < n; ++j) {
                var pJ = argSP.productions.get(j);
                tempChildren[i] = pJ;
                if (i + 1 < tempChildren.length) {
                    if (!produceASTs(s, tempChildren, argumentSymbols, i + 1, k, newProds, maxNewProductions)) {
                        return false;
                    }
                } else {
                    if (newProds.size() >= maxNewProductions) {
                        return false;
                    }
                    newProds.add(ASTNode.make(s, List.of(tempChildren)));
                }
            }
            return true;
        }
    }

    private static SymbolProductions eProductions = new SymbolProductions(Symbol.E);
    private static SymbolProductions bProductions = new SymbolProductions(Symbol.B);

    static {
        for (var s : eProductions.terminals) {
            eProductions.productions.add(ASTNode.make(s, ASTNode.NO_CHILDREN));
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

    private boolean validate(List<Example> examples, ASTNode program) {
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