package synth.algorithms.ast;

import synth.core.Environment;

public abstract class BoolNode extends AstNode {
    protected BoolNode(AstNode... children) {
        super(children);
    }

    public int evalExpr(Environment env) {
        throw new UnsupportedOperationException("Bool node cannot evaluate as Expr");
    }
}
