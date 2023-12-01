package synth.algorithms.ast;

import synth.core.Environment;

public abstract class ExprNode extends AstNode {
    public ExprNode(AstNode... children) {
        super(children);
    }

    public boolean evalBool(Environment env) {
        throw new UnsupportedOperationException("Expr node cannot evaluate as Bool");
    }
}
