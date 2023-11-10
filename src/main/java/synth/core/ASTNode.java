package synth.core;

import java.util.List;

import synth.dsl.Symbol;

public class ASTNode {
    public static final List<ASTNode> NO_CHILDREN = List.of();
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

    public ASTNode withChild(int index, ASTNode newChild) {
        if (this.children.size() == 1 && index == 0) {
            return new ASTNode(symbol, List.of(newChild));
        } else if (this.children.size() == 2) {
            switch (index) {
                case 0:
                    return new ASTNode(symbol, List.of(newChild, children.get(1)));
                case 1:
                    return new ASTNode(symbol, List.of(children.get(0), newChild));
            }
        }
        ASTNode[] newChildren = children.toArray(ASTNode[]::new);
        newChildren[index] = newChild;
        return new ASTNode(symbol, List.of(newChildren));
    }

    public ASTNode withChildren(int index, List<ASTNode> newChildren) {
        return new ASTNode(symbol, newChildren);
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
