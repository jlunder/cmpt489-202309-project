package synth.algorithms.ast;

import java.util.List;

import synth.core.*;
import synth.dsl.Symbol;

public class AndNode extends BoolNode {
    private final List<AstNode> children;
    private ParseNode reified = null;

    public AndNode(BoolNode left, BoolNode right) {
        children = List.of(left, right);
    }

    public List<AstNode> children() {
        return children;
    }

    public boolean evalBool(Environment env) {
        return children.get(0).evalBool(env) && children.get(1).evalBool(env);
    }

    public ParseNode reify() {
        if (this.reified == null) {
            this.reified = new ParseNode(Symbol.And, List.of(children.get(0).reify(), children.get(1).reify()));
        }
        return this.reified;
    }
}
