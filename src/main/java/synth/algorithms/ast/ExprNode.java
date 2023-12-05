package synth.algorithms.ast;

import synth.algorithms.representation.ExprRepresentation;
import synth.core.Environment;
import synth.core.ParseNode;

public abstract class ExprNode extends AstNode implements ExprRepresentation {
    public ExprNode(AstNode... children) {
        super(children);
    }

    public boolean evalBool(Environment env) {
        throw new UnsupportedOperationException("Expr node cannot evaluate as Bool");
    }

    public ExprNode reifyAsExprAst() {
        return this;
    }

    public ParseNode reifyAsExprParse() {
        return reify();
    }
}
