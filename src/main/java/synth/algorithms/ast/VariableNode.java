package synth.algorithms.ast;

import synth.core.*;
import synth.dsl.Symbol;

public class VariableNode extends ExprNode {
    public static ParseNode REIFIED_X = ParseNode.VAR_X;
    public static ParseNode REIFIED_Y = ParseNode.VAR_Y;
    public static ParseNode REIFIED_Z = ParseNode.VAR_Z;

    private final Symbol variable;

    public VariableNode(Symbol variable) {
        assert variable == Symbol.VarX || variable == Symbol.VarY || variable == Symbol.VarZ;
        this.variable = variable;
        switch (variable) {
            case VarX:
                this.reified = REIFIED_X;
                break;
            case VarY:
                this.reified = REIFIED_Y;
                break;
            case VarZ:
                this.reified = REIFIED_Z;
                break;
            default:
                assert "variable should be VarX, VarY, or VarZ" == null;
                break;
        }
    }

    public Symbol variable() {
        return variable;
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

    public AstNode withChildren(AstNode... children) {
        return new VariableNode(variable);
    }
}
