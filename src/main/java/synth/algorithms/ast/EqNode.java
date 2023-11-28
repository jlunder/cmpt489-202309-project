package synth.algorithms.ast;

import java.util.List;

import synth.core.*;
import synth.dsl.Symbol;

public class EqNode extends BoolNode {
    private final List<AstNode> children;
    private ParseNode reified = null;

    public EqNode(ExprNode left, ExprNode right) {
        children = List.of(left, right);
    }

    public List<AstNode> children() {
        return children;
    }

    public boolean evalBool(Environment env) {
        return children.get(0).evalExpr(env) == children.get(1).evalExpr(env);
    }

    public ParseNode reified() {
        if (this.reified == null) {
            this.reified = new ParseNode(Symbol.Eq, List.of(children.get(0).reified(), children.get(1).reified()));
        }
        return this.reified;
    }
}
