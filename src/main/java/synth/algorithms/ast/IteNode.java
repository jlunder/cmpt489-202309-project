package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class IteNode extends ExprNode {
    public IteNode(AstNode... children) {
        super(children);
        assert children.length == 3 && (children[0] instanceof BoolNode) && (children[1] instanceof ExprNode)
                && (children[2] instanceof ExprNode);
    }

    public int evalExpr(Environment env) {
        if (child(0).evalBool(env)) {
            return child(1).evalExpr(env);
        } else {
            return child(2).evalExpr(env);
        }
    }

    protected ParseNode makeReified() {
        return new ParseNode(Symbol.Ite, List.of(child(0).reify(), child(1).reify(), child(2).reify()));
    }

    public AstNode withChildren(AstNode... children) {
        return new IteNode(children);
    }
}
