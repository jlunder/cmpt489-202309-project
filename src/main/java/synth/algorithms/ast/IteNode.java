package synth.algorithms.ast;

import java.util.List;

import synth.core.*;
import synth.dsl.Symbol;

public class IteNode extends ExprNode {
    private final List<AstNode> children;
    private ParseNode reified = null;

    public IteNode(BoolNode cond, ExprNode ifCond, ExprNode elseCond) {
        children = List.of(cond, ifCond, elseCond);
    }

    public List<AstNode> children() {
        return children;
    }

    public int evalExpr(Environment env) {
        if (children.get(0).evalBool(env)) {
            return children.get(1).evalExpr(env);
        } else {
            return children.get(2).evalExpr(env);
        }
    }

    public ParseNode reify() {
        if (this.reified == null) {
            this.reified = new ParseNode(Symbol.Ite,
                    List.of(children.get(0).reify(), children.get(1).reify(), children.get(2).reify()));
        }
        return this.reified;
    }
}
