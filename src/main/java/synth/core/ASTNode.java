package synth.core;

import synth.cfg.Symbol;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

public class ASTNode {
    private final Symbol symbol;
    private final List<ASTNode> children;

    public ASTNode(Symbol symbol, List<ASTNode> children) {
        this.symbol = symbol;
        this.children = children;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public ASTNode getChild(int index) {
        return children.get(index);
    }

    public static ASTNode make(Symbol symbol, List<ASTNode> children) {
        return new ASTNode(symbol, children);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(symbol);
        String separator = "";
        if (!children.isEmpty()) {
            builder.append("(");
            for (ASTNode child : children) {
                builder.append(separator);
                separator = ", ";
                builder.append(child);
            }
            builder.append(")");
        }
        return builder.toString();
    }
}
