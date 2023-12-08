package synth.algorithms.mcmc;

import synth.algorithms.ast.*;
import synth.algorithms.representation.*;
import synth.core.Environment;
import synth.core.ParseNode;
import synth.dsl.*;

public class McmcProgramRepresentation implements ExprRepresentation, BoolRepresentation{
    private Symbol[] program;

    public Symbol[] program() {return program;}

    public McmcProgramRepresentation(Symbol[] program) {
        this.program = program;
    }

    @Override
    public BoolNode reifyAsBoolAst() {
        return Asts.makeBoolAstFromParse(Semantics.makeBoolParseTreeFromPostOrder(program));
    }

    @Override
    public ParseNode reifyAsBoolParse() {
        return Semantics.makeBoolParseTreeFromPostOrder(program);
    }

    @Override
    public boolean evalBool(Environment env) {
        return Semantics.evaluateBoolPostOrder(program, env);
    }

    @Override
    public ExprNode reifyAsExprAst() {
        return Asts.makeExprAstFromParse(Semantics.makeExprParseTreeFromPostOrder(program));
    }

    @Override
    public ParseNode reifyAsExprParse() {
        return Semantics.makeExprParseTreeFromPostOrder(program);
    }

    @Override
    public int evalExpr(Environment env) {
        return Semantics.evaluateExprPostOrder(program, env);
    }
    
}
