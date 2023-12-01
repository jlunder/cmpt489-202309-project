package synth.algorithms.ast;

import java.util.Arrays;

import synth.core.*;
import synth.dsl.Symbol;

public class AddNode extends ExprNode {
    public AddNode(AstNode... children) {
        super(children);
        assert children.length >= 2 && Arrays.stream(children).allMatch(c -> c instanceof ExprNode);
    }

    public int evalExpr(Environment env) {
        int accum = 0;
        for (int i = 0; i < numChildren(); ++i) {
            accum += child(i).evalExpr(env);
        }
        return accum;
    }

    protected ParseNode makeReified() {
        return makeReifiedAssociativeBinaryOperator(Symbol.Add);
    }

    public AstNode withChildren(AstNode... children) {
        return new AddNode(children);
    }
}
