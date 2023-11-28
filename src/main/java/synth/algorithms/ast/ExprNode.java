package synth.algorithms.ast;

import synth.core.Environment;

abstract class ExprNode extends AstNode {
    public boolean evalBool(Environment env) {
        throw new IllegalArgumentException("Expr node cannot evaluate as Bool");
    }
}