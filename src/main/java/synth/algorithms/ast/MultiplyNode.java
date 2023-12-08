package synth.algorithms.ast;

import java.util.Arrays;

import synth.core.Environment;
import synth.core.ParseNode;
import synth.dsl.*;

public class MultiplyNode extends ExprNode {
    public MultiplyNode(AstNode... children) {
        super(children);
        assert children.length >= 2 && Arrays.stream(children).allMatch(c -> c instanceof ExprNode);
    }

    public int evalExpr(Environment env) {
        int accum = 1;
        for (int i = 0; i < numChildren(); ++i) {
            accum *= child(i).evalExpr(env);
        }
        return accum;
    }

    protected ParseNode makeReified() {
        return makeReifiedAssociativeBinaryOperator(Symbol.Multiply);
    }

    public AstNode withChildren(AstNode... children) {
        return new MultiplyNode(children);
    }
}
