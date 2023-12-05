package synth.algorithms.ast;

import synth.algorithms.representation.BoolRepresentation;
import synth.core.Environment;
import synth.core.ParseNode;

public abstract class BoolNode extends AstNode implements BoolRepresentation {
    protected BoolNode(AstNode... children) {
        super(children);
    }

    public int evalExpr(Environment env) {
        throw new UnsupportedOperationException("Bool node cannot evaluate as Expr");
    }

    public BoolNode reifyAsBoolAst() {
        return this;
    }

    public ParseNode reifyAsBoolParse() {
        return reify();
    }
}
