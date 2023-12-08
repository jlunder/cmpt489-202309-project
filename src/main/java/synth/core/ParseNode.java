package synth.core;

import java.util.List;

import synth.dsl.Symbol;

public class ParseNode {
    public static final List<ParseNode> NO_CHILDREN = List.of();

    public static final ParseNode CONST_1 = new ParseNode(Symbol.Const1, ParseNode.NO_CHILDREN);
    public static final ParseNode CONST_2 = new ParseNode(Symbol.Const2, ParseNode.NO_CHILDREN);
    public static final ParseNode CONST_3 = new ParseNode(Symbol.Const3, ParseNode.NO_CHILDREN);

    public static final ParseNode VAR_X = new ParseNode(Symbol.VarX, ParseNode.NO_CHILDREN);
    public static final ParseNode VAR_Y = new ParseNode(Symbol.VarY, ParseNode.NO_CHILDREN);
    public static final ParseNode VAR_Z = new ParseNode(Symbol.VarZ, ParseNode.NO_CHILDREN);

    public static final ParseNode CONST_TRUE = new ParseNode(Symbol.Eq, List.of(CONST_1, CONST_1));
    public static final ParseNode CONST_FALSE = new ParseNode(Symbol.Eq, List.of(CONST_1, CONST_2));

    private final Symbol symbol;
    private final List<ParseNode> children;

    public ParseNode(Symbol symbol) {
        assert symbol.operatorArguments().size() == 0;
        this.symbol = symbol;
        this.children = NO_CHILDREN;
    }

    public ParseNode(Symbol symbol, List<ParseNode> children) {
        assert children.size() == symbol.operatorArguments().size();
        this.symbol = symbol;
        this.children = children;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public List<ParseNode> getChildren() {
        return children;
    }

    public ParseNode getChild(int index) {
        return children.get(index);
    }

    public ParseNode withChild(int index, ParseNode newChild) {
        if (this.children.size() == 1 && index == 0) {
            return new ParseNode(symbol, List.of(newChild));
        } else if (this.children.size() == 2) {
            switch (index) {
                case 0:
                    return new ParseNode(symbol, List.of(newChild, children.get(1)));
                case 1:
                    return new ParseNode(symbol, List.of(children.get(0), newChild));
            }
        }
        ParseNode[] newChildren = children.toArray(ParseNode[]::new);
        newChildren[index] = newChild;
        return new ParseNode(symbol, List.of(newChildren));
    }

    public ParseNode withChildren(int index, List<ParseNode> newChildren) {
        return new ParseNode(symbol, newChildren);
    }

    public static ParseNode make(Symbol symbol, List<ParseNode> children) {
        return new ParseNode(symbol, children);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(symbol);
        String separator = "";
        if (!children.isEmpty()) {
            builder.append("(");
            for (ParseNode child : children) {
                builder.append(separator);
                separator = ", ";
                builder.append(child);
            }
            builder.append(")");
        }
        return builder.toString();
    }
}
