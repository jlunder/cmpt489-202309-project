package synth.algorithms.ast;

import java.util.*;

import synth.core.*;

public class BoolMarkerNode extends BoolNode {
    private final int marker;

    public BoolMarkerNode(int marker) {
        this.marker = marker;
    }

    public int marker() {
        return marker;
    }

    public boolean evalBool(Environment env) {
        throw new UnsupportedOperationException("markers cannot be evaluated");
    }

    public ParseNode reify() {
        throw new UnsupportedOperationException("markers cannot be reified");
    }

    public AstNode withChildren(AstNode... children) {
        return new BoolMarkerNode(marker);
    }

    public AstNode substituteMarkers(Map<Integer, AstNode> substitution) {
        return (BoolNode) substitution.getOrDefault(marker, this);
    }
}
