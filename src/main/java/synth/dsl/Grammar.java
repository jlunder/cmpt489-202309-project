package synth.dsl;

import java.util.List;

public class Grammar {
    public static Symbol startSymbol() {
        return Symbol.E;
    }

    public static boolean isTerminal(Symbol symbol) {
        return symbol.isTerminal();
    }

    public static boolean isNonTerminal(Symbol symbol) {
        return symbol.isTerminal();
    }

    public static Symbol getReturnSymbol(Symbol symbol) {
        return symbol.returnSymbol();
    }

    public static List<Symbol> getProductionOperators(Symbol symbol) {
        return symbol.productionOperators();
    }

    public static List<Symbol> getOperatorArguments(Symbol symbol) {
        return symbol.operatorArguments();
    }
}
