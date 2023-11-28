package synth.algorithms.ast;

import synth.core.Environment;

abstract class BoolNode extends AstNode {
    public int evalExpr(Environment env) {
        throw new IllegalArgumentException("Bool node cannot evaluate as Expr");
    }
}
