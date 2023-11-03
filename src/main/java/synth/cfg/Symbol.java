package synth.cfg;

public abstract class Symbol {
    protected final String name;

    public Symbol(String name) {
        this.name = name.intern();
    }

    public String getName() {
        return name;
    }

    public abstract boolean isTerminal();

    public abstract boolean isNonTerminal();
}
