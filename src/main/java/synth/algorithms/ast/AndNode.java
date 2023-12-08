package synth.algorithms.ast;

import java.util.Arrays;

import synth.core.Environment;
import synth.core.ParseNode;
import synth.dsl.*;

public class AndNode extends BoolNode {
    public AndNode(AstNode... children) {
        super(children);
        assert children.length >= 2 && Arrays.stream(children).allMatch(c -> c instanceof BoolNode);
    }

    public boolean evalBool(Environment env) {
        for (int i = 0; i < numChildren(); ++i) {
            if (!child(i).evalBool(env)) {
                return false;
            }
        }
        return true;
    }

    protected ParseNode makeReified() {
        return makeReifiedAssociativeBinaryOperator(Symbol.And);
    }

    public AstNode withChildren(AstNode... children) {
        return new AndNode(children);
    }
}
