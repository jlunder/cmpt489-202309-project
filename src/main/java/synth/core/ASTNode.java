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

    private record MakeParams(Symbol symbol, List<ASTNode> children) {
    }

    private static final HashMap<Symbol, WeakReference<ASTNode>> terminalsMemo = new HashMap<>();
    private static final HashMap<MakeParams, WeakReference<ASTNode>> nonTerminalsMemo = new HashMap<>();

    public static ASTNode make(Symbol symbol, List<ASTNode> children) {
        ASTNode node = null;
        if (children == null || children.isEmpty()) {
            var w = terminalsMemo.get(symbol);
            if (w != null) {
                node = w.get();
            }
            if (node == null) {
                node = new ASTNode(symbol, List.of());
                terminalsMemo.put(symbol, new WeakReference<ASTNode>(node));
            }
        } else {
            var p = new MakeParams(symbol, children);
            var w = nonTerminalsMemo.get(p);
            if (w != null) {
                node = w.get();
            }
            if (node == null) {
                node = new ASTNode(symbol, children);
                nonTerminalsMemo.put(p, new WeakReference<ASTNode>(node));
            }
        }
        return node;
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
