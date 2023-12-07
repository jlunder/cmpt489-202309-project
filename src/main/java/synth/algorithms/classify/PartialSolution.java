package synth.algorithms.classify;

import synth.algorithms.ast.ExprNode;
import synth.algorithms.representation.*;
import synth.core.Environment;
import synth.core.ParseNode;

public class PartialSolution implements ExprRepresentation {
    private ExprRepresentation solution;
    private Classification application;

    public ExprRepresentation solution() {
        return solution;
    }

    public Classification application() {
        return application;
    }

    public PartialSolution(ExprRepresentation solution, Classification application) {
        this.solution = solution;
        this.application = application;
    }

    @Override
    public ExprNode reifyAsExprAst() {
        return solution.reifyAsExprAst();
    }

    @Override
    public ParseNode reifyAsExprParse() {
        return solution.reifyAsExprParse();
    }

    @Override
    public int evalExpr(Environment env) {
        return solution.evalExpr(env);
    }
}
