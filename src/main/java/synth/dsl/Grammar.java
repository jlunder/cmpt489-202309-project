package synth.dsl;

import java.util.List;

public class Grammar {
    public static Symbol startSymbol() {
        return Symbol.E;
    }

    public static boolean isTerminalProduction(Symbol symbol) {
        return !symbol.isTerminalProduction();
    }

    public static boolean requiresArguments(Symbol symbol) {
        return symbol.requiresArguments();
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
