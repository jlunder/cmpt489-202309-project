package synth.algorithms.ast;

import synth.core.*;
import synth.dsl.*;

public class Asts {
    ExprNode optimizeExprAst(ExprNode node) {
        // fold constants
        // merge nested associative nodes
        // canonicalize order
        return node;
    }

    BoolNode optimizeBoolAst(BoolNode node) {
        // fold constants
        // merge nested associative nodes
        // canonicalize order
        return node;
    }

    ParseNode optimizeParse(ParseNode node) {
        if (node.getSymbol().returnSymbol() == Symbol.E) {
            return optimizeExprAst(makeExprAstFromParse(node)).reify();
        } else if (node.getSymbol().returnSymbol() == Symbol.B) {
            return optimizeExprAst(makeExprAstFromParse(node)).reify();
        } else {
            throw new UnsupportedOperationException("Unrecognizable node type: " + node.getSymbol());
        }

    }

    ExprNode makeExprAstFromParse(ParseNode parse) {
        switch (parse.getSymbol()) {
            case Const1:
                return ExprConstNode.CONST_1;
            case Const2:
                return ExprConstNode.CONST_2;
            case Const3:
                return ExprConstNode.CONST_3;
            case VarX:
                return VariableNode.VAR_X;
            case VarY:
                return VariableNode.VAR_Y;
            case VarZ:
                return VariableNode.VAR_Z;
            case Add:
                return new AddNode(makeExprAstFromParse(parse.getChild(0)), makeExprAstFromParse(parse.getChild(1)));
            case Multiply:
                return new MultiplyNode(makeExprAstFromParse(parse.getChild(0)),
                        makeExprAstFromParse(parse.getChild(1)));
            case Ite:
                return new IteNode(makeBoolAstFromParse(parse.getChild(0)), makeExprAstFromParse(parse.getChild(1)),
                        makeExprAstFromParse(parse.getChild(2)));
            default:
                throw new UnsupportedOperationException("Cannot convert symbol as expr node: " + parse.getSymbol());
        }
    }

    BoolNode makeBoolAstFromParse(ParseNode parse) {
        switch (parse.getSymbol()) {
            case Lt:
                return new LtNode(makeExprAstFromParse(parse.getChild(0)), makeExprAstFromParse(parse.getChild(1)));
            case Eq:
                return new EqNode(makeExprAstFromParse(parse.getChild(0)), makeExprAstFromParse(parse.getChild(1)));
            case And:
                return new AndNode(makeBoolAstFromParse(parse.getChild(0)), makeBoolAstFromParse(parse.getChild(1)));
            case Or:
                return new OrNode(makeBoolAstFromParse(parse.getChild(0)), makeBoolAstFromParse(parse.getChild(1)));
            case Not:
                return new NotNode(makeBoolAstFromParse(parse.getChild(0)));
            default:
                throw new UnsupportedOperationException("Cannot convert symbol as bool node: " + parse.getSymbol());
        }
    }
}
