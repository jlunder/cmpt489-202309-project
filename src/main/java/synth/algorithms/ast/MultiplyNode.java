package synth.algorithms.ast;

import java.util.List;

import synth.core.*;
import synth.dsl.Symbol;

public class MultiplyNode extends ExprNode {
    private final List<AstNode> children;
    private ParseNode reified = null;

    public MultiplyNode(ExprNode left, ExprNode right) {
        children = List.of(left, right);
    }

    public List<AstNode> children() {
        return children;
    }

    public int evalExpr(Environment env) {
        return children.get(0).evalExpr(env) * children.get(1).evalExpr(env);
    }

    public ParseNode reified() {
        if (this.reified == null) {
            this.reified = new ParseNode(Symbol.Multiply,
                    List.of(children.get(0).reified(), children.get(1).reified()));
        }
        return this.reified;
    }
}
