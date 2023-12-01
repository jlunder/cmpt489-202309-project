package synth.algorithms.ast;

import java.util.List;

import synth.core.*;
import synth.dsl.Symbol;

public class BoolConstNode extends BoolNode {
    private static final ParseNode REIFIED_TRUE = new ParseNode(Symbol.Eq,
            List.of(ExprConstNode.REIFIED_1, ExprConstNode.REIFIED_1));
    private static final ParseNode REIFIED_FALSE = new ParseNode(Symbol.Eq,
            List.of(ExprConstNode.REIFIED_1, ExprConstNode.REIFIED_2));

    private boolean value;

    public BoolConstNode(boolean value) {
        this.value = value;
        if (value) {
            reified = REIFIED_TRUE;
        } else {
            reified = REIFIED_FALSE;
        }
    }

    public boolean value() {
        return this.value;
    }

    public boolean evalBool(Environment env) {
        return value;
    }

    public AstNode withChildren(AstNode... children) {
        return new BoolConstNode(value);
    }
}
