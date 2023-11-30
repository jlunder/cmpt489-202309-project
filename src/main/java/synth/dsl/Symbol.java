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
    Ite("Ite", E, List.of(B, E, E)),
    Add("Add", E, List.of(E, E)),
    Multiply("Multiply", E, List.of(E, E)),
    Lt("Lt", B, List.of(E, E)),
    Eq("Eq", B, List.of(E, E)),
    And("And", B, List.of(B, B)),
    Or("Or", B, List.of(B, B)),
    Not("Not", B, List.of(B));

    private final String name;
    private final boolean terminal;
    private final Symbol returnSym;
    private List<Symbol> productionOps;
    private final List<Symbol> operatorArgs;

    private Symbol(String name, Symbol returnSym, List<Symbol> operatorArgs) {
        assert name != null && !name.isEmpty();
        assert (returnSym == null) == (operatorArgs == null);
        this.name = name;
        this.terminal = operatorArgs != null && operatorArgs.isEmpty();
        this.returnSym = returnSym != null ? returnSym : this;
        this.operatorArgs = operatorArgs;
    }

    public String getName() {
        return name;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isNonTerminal() {
        return !terminal;
    }

    Symbol returnSymbol() {
        return returnSym;
    }

    List<Symbol> productionOperators() {
        assert !this.terminal;
        if (productionOps == null) {
            // This is fairly threadsafe: worst case is we have multiple identical operator
            // lists floating about
            productionOps = List.of(Arrays.stream(values()).filter(sym -> sym != this && sym.returnSym == this).toArray(Symbol[]::new));
        }
        return productionOps;
    }

    List<Symbol> operatorArguments() {
        assert this.terminal;
        return operatorArgs;
    }
}
