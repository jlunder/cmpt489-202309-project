package synth.algorithms.classify;

import java.util.List;

import synth.algorithms.ast.*;
import synth.algorithms.representation.*;
import synth.core.*;
import synth.dsl.*;

public class DecisionTree implements ExprRepresentation {
    private Discriminator discriminator;
    private ExprRepresentation thenBranch;
    private ExprRepresentation elseBranch;

    public DecisionTree(Discriminator discriminator, ExprRepresentation thenBranch, ExprRepresentation elseBranch) {
        this.discriminator = discriminator;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Discriminator discriminator() {
        return discriminator;
    }

    public ExprRepresentation thenBranch() {
        return thenBranch;
    }

    public ExprRepresentation elseBranch() {
        return elseBranch;
    }

    public ExprRepresentation classify(Environment input) {
        if (discriminator.evalBool(input)) {
            if (thenBranch instanceof DecisionTree) {
                return ((DecisionTree) thenBranch).classify(input);
            } else {
                return thenBranch;
            }
        } else {
            if (elseBranch instanceof DecisionTree) {
                return ((DecisionTree) elseBranch).classify(input);
            } else {
                return elseBranch;
            }
        }
    }

    @Override
    public ExprNode reifyAsExprAst() {
        return new IteNode(discriminator.reifyAsBoolAst(), thenBranch.reifyAsExprAst(),
                elseBranch.reifyAsExprAst());
    }

    @Override
    public ParseNode reifyAsExprParse() {
        return new ParseNode(Symbol.Ite, List.of(discriminator.reifyAsBoolParse(),
                thenBranch.reifyAsExprParse(), elseBranch.reifyAsExprParse()));
    }

    @Override
    public int evalExpr(Environment env) {
        if (discriminator.condition().evalBool(env)) {
            return thenBranch.evalExpr(env);
        } else {
            return elseBranch.evalExpr(env);
        }
    }
}
