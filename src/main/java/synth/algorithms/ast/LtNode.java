package synth.algorithms.ast;

import java.util.*;

import synth.core.Environment;
import synth.core.ParseNode;
import synth.dsl.*;

public class LtNode extends BoolNode {
    public LtNode(AstNode... children) {
        super(children);
        assert children.length == 2 && (children[0] instanceof ExprNode) && (children[1] instanceof ExprNode);
    }

    public boolean evalBool(Environment env) {
        return child(0).evalExpr(env) < child(1).evalExpr(env);
    }

    protected ParseNode makeReified() {
        return new ParseNode(Symbol.Lt, List.of(child(0).reify(), child(1).reify()));
    }

    public AstNode withChildren(AstNode... children) {
        return new LtNode(children[0], children[1]);
    }
}
