package synth.algorithms.ast;

import java.util.List;

import synth.core.*;
import synth.dsl.Symbol;

public class NotNode extends BoolNode {
    private final List<AstNode> children;
    private ParseNode reified = null;

    public NotNode(BoolNode inner) {
        children = List.of(inner);
    }

    public List<AstNode> children() {
        return children;
    }

    public boolean evalBool(Environment env) {
        return children.get(0).evalBool(env);
    }

    public ParseNode reify() {
        if (this.reified == null) {
            this.reified = new ParseNode(Symbol.Not, List.of(children.get(0).reify()));
        }
        return this.reified;
    }
}
