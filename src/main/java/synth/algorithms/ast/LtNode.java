package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class LtNode extends BoolNode {
    private final List<AstNode> children;
    private ParseNode reified = null;

    public LtNode(ExprNode left, ExprNode right) {
        children = List.of(left, right);
    }

    public List<AstNode> children() {
        return children;
    }

    public boolean evalBool(Environment env) {
        return children.get(0).evalExpr(env) < children.get(1).evalExpr(env);
    }

    public ParseNode reify() {
        if (this.reified == null) {
            this.reified = new ParseNode(Symbol.Lt, List.of(children.get(0).reify(), children.get(1).reify()));
        }
        return this.reified;
    }

    public AstNode substituteMarkers(Map<Integer, AstNode> substitution) {
        return new LtNode((ExprNode) children.get(0).substituteMarkers(substitution),
                (ExprNode) children.get(1).substituteMarkers(substitution));
    }
}
