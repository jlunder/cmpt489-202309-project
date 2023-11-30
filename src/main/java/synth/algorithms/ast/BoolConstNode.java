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
    }

    public boolean value() {
        return this.value;
    }

    public List<AstNode> children() {
        return NO_CHILDREN;
    }

    public boolean evalBool(Environment env) {
        return value;
    }

    public ParseNode reify() {
        if (this.value) {
            return REIFIED_TRUE;
        } else {
            return REIFIED_FALSE;
        }
    }
}
