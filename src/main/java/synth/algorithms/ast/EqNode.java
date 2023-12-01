package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class EqNode extends BoolNode {
    public EqNode(AstNode... children) {
        super(children);
        assert children.length == 2 && (children[0] instanceof ExprNode) && (children[1] instanceof ExprNode);
    }

    public boolean evalBool(Environment env) {
        return child(0).evalExpr(env) == child(1).evalExpr(env);
    }

    protected ParseNode makeReified() {
        return new ParseNode(Symbol.Eq, List.of(child(0).reify(), child(1).reify()));
    }

    public AstNode withChildren(AstNode... children) {
        return new EqNode(children);
    }
}
