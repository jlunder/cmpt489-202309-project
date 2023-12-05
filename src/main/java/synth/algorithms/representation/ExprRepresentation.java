package synth.algorithms.representation;

import synth.algorithms.ast.*;
import synth.core.*;

public interface ExprRepresentation {
    public ExprNode reifyAsExprAst();
    public ParseNode reifyAsExprParse();
    public int evalExpr(Environment env);
}
