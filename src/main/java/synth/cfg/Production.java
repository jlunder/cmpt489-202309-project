package synth.cfg;

import java.util.List;

/**
 * A production is of the form
 * return symbol ::= operator( argSymbol, argSymbol, ... )
 */
public class Production {
    /**
     * return symbol
     */
    private Symbol retSymbol;
    /**
     * operator symbol
     */
    private Symbol operator;
    /**
     * argument symbols
     */
    private List<Symbol> argSymbols;

    public Production(Symbol returnSymbol, Symbol operator, List<Symbol> argumentSymbols) {
        assert returnSymbol.isNonTerminal();
        assert operator.isTerminal();
        this.retSymbol = returnSymbol;
        this.operator = operator;
        this.argSymbols = argumentSymbols;
    }

    public Symbol getReturnSymbol() {
        return retSymbol;
    }

    public Symbol getOperator() {
        return operator;
    }

    public List<Symbol> getArgumentSymbols() {
        return argSymbols;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(retSymbol).append(" ::= ").append(operator);
        String separator = "";
        if (!argSymbols.isEmpty()) {
            builder.append("(");
            for (Symbol argSymbol : argSymbols) {
                builder.append(separator);
                separator = ", ";
                builder.append(argSymbol);
            }
            builder.append(")");
        }
        return builder.toString();
    }
}
