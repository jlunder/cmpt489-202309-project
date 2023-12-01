package synth.algorithms.ast;

import java.util.*;

import synth.core.*;

public class ExprMarkerNode extends ExprNode {
    private final int marker;

    public ExprMarkerNode(int marker) {
        this.marker = marker;
    }

    public int marker() {
        return marker;
    }

    public int evalExpr(Environment env) {
        throw new UnsupportedOperationException("markers cannot be evaluated");
    }

    public ParseNode reify() {
        throw new UnsupportedOperationException("markers cannot be reified");
    }

    public AstNode withChildren(AstNode... children) {
        return new ExprMarkerNode(marker);
    }

    public AstNode substituteMarkers(Map<Integer, AstNode> substitution) {
        return (ExprNode) substitution.getOrDefault(marker, this);
    }
}
