package synth.algorithms.ast;

import synth.core.*;

public class BoolConstNode extends BoolNode {
    private static final ParseNode REIFIED_TRUE = ParseNode.CONST_TRUE;
    private static final ParseNode REIFIED_FALSE = ParseNode.CONST_FALSE;

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
