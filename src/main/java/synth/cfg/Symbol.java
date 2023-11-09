package synth.cfg;

public enum Symbol {
    E("E", false),
    Const1("1", true),
    Const2("2", true),
    Const3("3", true),
    VarX("x", true),
    VarY("y", true),
    VarZ("z", true),
    Ite("Ite", true),
    Add("Add", true),
    Multiply("Multiply", true),
    B("B", false),
    Lt("Lt", true),
    Eq("Eq", true),
    And("And", true),
    Or("Or", true),
    Not("Not", true);

    private final String name;
    private final boolean terminal;

    private Symbol(String name, boolean terminal) {
        this.name = name;
        this.terminal = terminal;
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
}
