package synth.core;

public class Program {
    private final ParseNode root;

    public Program(ParseNode root) {
        this.root = root;
    }

    public ParseNode getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return String.valueOf(root);
    }
}
