package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

class VariableNode extends ExprNode {
    public static ParseNode REIFIED_X = new ParseNode(Symbol.VarX, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_Y = new ParseNode(Symbol.VarY, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_Z = new ParseNode(Symbol.VarZ, ParseNode.NO_CHILDREN);

    private final Symbol variable;

    public VariableNode(Symbol variable) {
        assert variable == Symbol.VarX || variable == Symbol.VarY || variable == Symbol.VarZ;
        this.variable = variable;
    }

    public int variable() {
        return this.variable();
    }

    public List<AstNode> children() {
        return NO_CHILDREN;
    }

    public int evalExpr(Environment env) {
        switch (this.variable) {
            case VarX:
                return env.x();
            case VarY:
                return env.y();
            case VarZ:
                return env.z();
            default:
                throw new IllegalArgumentException("variable should be VarX, VarY, or VarZ");
        }
    }

    public ParseNode reified() {
        switch (this.variable) {
            case VarX:
                return REIFIED_X;
            case VarY:
                return REIFIED_Y;
            case VarZ:
                return REIFIED_Z;
            default:
                throw new IllegalArgumentException("variable should be VarX, VarY, or VarZ");
        }
    }
}
