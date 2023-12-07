package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class NotNode extends BoolNode {
    public NotNode(AstNode... children) {
        super(children);
        assert children.length == 1 && (children[0] instanceof BoolNode);
    }

    public boolean evalBool(Environment env) {
        return !child(0).evalBool(env);
    }

    protected ParseNode makeReified() {
        return new ParseNode(Symbol.Not, List.of(child(0).reify()));
    }

    public AstNode withChildren(AstNode... children) {
        return new NotNode(children[0]);
    }
}
