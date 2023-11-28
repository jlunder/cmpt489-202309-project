package synth.algorithms.ast;

import java.util.List;

import synth.core.*;

public abstract class AstNode {
    public static final List<AstNode> NO_CHILDREN = List.of();

    public abstract List<AstNode> children();

    public abstract boolean evalBool(Environment env);

    public abstract int evalExpr(Environment env);

    public abstract ParseNode reified();
}
