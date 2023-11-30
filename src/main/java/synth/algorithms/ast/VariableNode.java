package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class VariableNode extends ExprNode {
    public static ParseNode REIFIED_X = new ParseNode(Symbol.VarX, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_Y = new ParseNode(Symbol.VarY, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_Z = new ParseNode(Symbol.VarZ, ParseNode.NO_CHILDREN);

    private final Symbol variable;

    public VariableNode(Symbol variable) {
        assert variable == Symbol.VarX || variable == Symbol.VarY || variable == Symbol.VarZ;
        this.variable = variable;
    }

    public Symbol variable() {
        return variable;
    }

    public List<AstNode> children() {
        return NO_CHILDREN;
    }

    public int evalExpr(Environment env) {
        switch (variable) {
            case VarX:
                return env.x();
            case VarY:
                return env.y();
            case VarZ:
                return env.z();
            default:
                assert "variable should be VarX, VarY, or VarZ" == null;
                return 1;
        }
    }

    public ParseNode reify() {
        switch (variable) {
            case VarX:
                return REIFIED_X;
            case VarY:
                return REIFIED_Y;
            case VarZ:
                return REIFIED_Z;
            default:
                assert "variable should be VarX, VarY, or VarZ" == null;
                return ExprConstNode.REIFIED_1;
        }
    }
}
