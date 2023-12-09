package synth.dsl;

import java.util.*;

public enum Symbol {
    E("E", null, null),
    B("B", null, null),
    Const1("1", E, List.of()),
    Const2("2", E, List.of()),
    Const3("3", E, List.of()),
    VarX("x", E, List.of()),
    VarY("y", E, List.of()),
    VarZ("z", E, List.of()),
    Add("Add", E, List.of(E, E)),
    Multiply("Multiply", E, List.of(E, E)),
    Ite("Ite", E, List.of(B, E, E)),
    Lt("Lt", B, List.of(E, E)),
    Eq("Eq", B, List.of(E, E)),
    And("And", B, List.of(B, B)),
    Or("Or", B, List.of(B, B)),
    Not("Not", B, List.of(B));

    private final String token;
    private final boolean terminal;
    private final Symbol returnSym;
    private List<Symbol> productionOps;
    private final List<Symbol> operatorArgs;

    private Symbol(String token, Symbol returnSym, List<Symbol> operatorArgs) {
        assert token != null && !token.isEmpty();
        assert (returnSym == null) == (operatorArgs == null);
        this.token = token;
        this.terminal = operatorArgs != null;
        this.returnSym = returnSym != null ? returnSym : this;
        this.operatorArgs = operatorArgs;
    }

    public String token() {
        return token;
    }

    public boolean requiresArguments() {
        return operatorArgs != null && operatorArgs.size() > 0;
    }

    public boolean isTerminalProduction() {
        return terminal;
    }

    public Symbol returnSymbol() {
        return returnSym;
    }

    public List<Symbol> productionOperators() {
        assert !terminal;
        if (productionOps == null) {
            // This is fairly threadsafe: worst case is we have multiple identical operator
            // lists floating about
            productionOps = List.of(Arrays.stream(values()).filter(sym -> sym != this && sym.returnSym == this).toArray(Symbol[]::new));
        }
        return productionOps;
    }

    public List<Symbol> operatorArguments() {
        assert terminal;
        return operatorArgs;
    }
}
