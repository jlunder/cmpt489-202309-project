package synth.util;

import java.util.List;

import synth.algorithms.ast.*;
import synth.core.*;
import synth.dsl.Symbol;

public class TestData {
    public static final ParseNode const1TestParseTree = new ParseNode(Symbol.Const1);
    public static final Symbol[] const1TestPostOrder = new Symbol[] { Symbol.Const1 };
    public static final ExprNode const1TestExprAst = new ExprConstNode(1);
    public static final Example[] const1TestExamples = new Example[] { new Example(new Environment(10, 15, 20), 1) };
    public static final int const1TestParseNodeSize = const1TestExamples.length;

    public static final ParseNode const2TestParseTree = new ParseNode(Symbol.Const2);
    public static final Symbol[] const2TestPostOrder = new Symbol[] { Symbol.Const2 };
    public static final ExprNode const2TestExprAst = new ExprConstNode(2);
    public static final Example[] const2TestExamples = new Example[] { new Example(new Environment(10, 15, 20), 2) };
    public static final int const2TestParseNodeSize = const2TestPostOrder.length;

    public static final ParseNode const3TestParseTree = new ParseNode(Symbol.Const3);
    public static final Symbol[] const3TestPostOrder = new Symbol[] { Symbol.Const3 };
    public static final ExprNode const3TestExprAst = new ExprConstNode(3);
    public static final Example[] const3TestExamples = new Example[] { new Example(new Environment(10, 15, 20), 3) };
    public static final int const3TestParseNodeSize = const3TestPostOrder.length;

    public static final ParseNode varXTestParseTree = new ParseNode(Symbol.VarX);
    public static final Symbol[] varXTestPostOrder = new Symbol[] { Symbol.VarX };
    public static final ExprNode varXTestExprAst = new VariableNode(Symbol.VarX);
    public static final Example[] varXTestExamples = new Example[] { new Example(new Environment(10, 15, 20), 10) };
    public static final int varXTestParseNodeSize = varXTestPostOrder.length;

    public static final ParseNode varYTestParseTree = new ParseNode(Symbol.VarY);
    public static final Symbol[] varYTestPostOrder = new Symbol[] { Symbol.VarY };
    public static final ExprNode varYTestExprAst = new VariableNode(Symbol.VarY);
    public static final Example[] varYTestExamples = new Example[] { new Example(new Environment(10, 15, 20), 15) };
    public static final int varYTestParseNodeSize = varYTestPostOrder.length;

    public static final ParseNode varZTestParseTree = new ParseNode(Symbol.VarZ);
    public static final Symbol[] varZTestPostOrder = new Symbol[] { Symbol.VarZ };
    public static final ExprNode varZTestExprAst = new VariableNode(Symbol.VarZ);
    public static final Example[] varZTestExamples = new Example[] { new Example(new Environment(10, 15, 20), 20) };
    public static final int varZTestParseNodeSize = varZTestPostOrder.length;

    public static final ParseNode addTestParseTree = new ParseNode(Symbol.Add,
            List.of(new ParseNode(Symbol.VarX),
                    new ParseNode(Symbol.VarY)));
    public static final Symbol[] addTestPostOrder = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Add };
    public static final ExprNode addTestExprAst = new AddNode(new VariableNode(Symbol.VarX),
            new VariableNode(Symbol.VarY));
    public static final Example[] addTestExamples = new Example[] { new Example(new Environment(10, 15, 20), 25) };
    public static final int addTestParseNodeSize = addTestPostOrder.length;

    public static final ParseNode multiplyTestParseTree = new ParseNode(Symbol.Multiply,
            List.of(new ParseNode(Symbol.VarZ),
                    new ParseNode(Symbol.Const2)));
    public static final Symbol[] multiplyTestPostOrder = new Symbol[] { Symbol.Const2, Symbol.VarZ, Symbol.Multiply };
    public static final ExprNode multiplyTestExprAst = new MultiplyNode(new VariableNode(Symbol.VarZ),
            new ExprConstNode(2));
    public static final Example[] multiplyTestExamples = new Example[] {
            new Example(new Environment(10, 15, 20), 40) };
    public static final int multiplyTestParseNodeSize = multiplyTestPostOrder.length;

    public static final ParseNode iteLtTestParseTree = new ParseNode(Symbol.Ite,
            List.of(new ParseNode(Symbol.Lt,
                    List.of(new ParseNode(Symbol.VarX),
                            new ParseNode(Symbol.VarY))),
                    new ParseNode(Symbol.Const1),
                    new ParseNode(Symbol.Const2)));
    public static final Symbol[] iteLtTestPostOrder = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarY,
            Symbol.VarX, Symbol.Lt, Symbol.Ite };
    public static final ExprNode iteLtTestExprAst = new IteNode(
            new LtNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)),
            new ExprConstNode(1),
            new ExprConstNode(2));
    public static final Example[] iteLtTestExamples = new Example[] {
            new Example(new Environment(10, 15, 20), 1),
            new Example(new Environment(15, 15, 20), 2),
            new Example(new Environment(20, 15, 20), 2) };
    public static final int iteLtTestParseNodeSize = iteLtTestPostOrder.length;

    public static final ParseNode iteEqTestParseTree = new ParseNode(Symbol.Ite,
            List.of(new ParseNode(Symbol.Eq,
                    List.of(new ParseNode(Symbol.VarX),
                            new ParseNode(Symbol.VarY))),
                    new ParseNode(Symbol.Const1),
                    new ParseNode(Symbol.Const2)));
    public static final Symbol[] iteEqTestPostOrder = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarY,
            Symbol.VarX, Symbol.Eq, Symbol.Ite };
    public static final ExprNode iteEqTestExprAst = new IteNode(
            new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)),
            new ExprConstNode(1),
            new ExprConstNode(2));
    public static final Example[] iteEqTestExamples = new Example[] {
            new Example(new Environment(10, 15, 20), 2),
            new Example(new Environment(15, 15, 20), 1),
            new Example(new Environment(20, 15, 20), 2) };
    public static final int iteEqTestParseNodeSize = iteEqTestPostOrder.length;

    public static final ParseNode iteAndTestParseTree = new ParseNode(Symbol.Ite,
            List.of(new ParseNode(Symbol.And,
                    List.of(new ParseNode(Symbol.Eq,
                            List.of(new ParseNode(Symbol.VarX),
                                    new ParseNode(Symbol.VarY))),
                            new ParseNode(Symbol.Eq,
                                    List.of(new ParseNode(Symbol.VarX),
                                            new ParseNode(Symbol.VarZ))))),
                    new ParseNode(Symbol.Const1),
                    new ParseNode(Symbol.Const2)));
    public static final Symbol[] iteAndTestPostOrder = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarZ,
            Symbol.VarX, Symbol.Eq, Symbol.VarY, Symbol.VarX, Symbol.Eq, Symbol.And, Symbol.Ite };
    public static final ExprNode iteAndTestExprAst = new IteNode(
            new AndNode(
                    new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)),
                    new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarZ))),
            new ExprConstNode(1),
            new ExprConstNode(2));
    public static final Example[] iteAndTestExamples = new Example[] {
            new Example(new Environment(1, 1, 1), 1),
            new Example(new Environment(1, 1, 2), 2),
            new Example(new Environment(1, 2, 1), 2),
            new Example(new Environment(1, 2, 2), 2) };
    public static final int iteAndTestParseNodeSize = iteAndTestPostOrder.length;

    public static final ParseNode iteOrTestParseTree = new ParseNode(Symbol.Ite,
            List.of(new ParseNode(Symbol.Or,
                    List.of(new ParseNode(Symbol.Eq,
                            List.of(new ParseNode(Symbol.VarX),
                                    new ParseNode(Symbol.VarY))),
                            new ParseNode(Symbol.Eq,
                                    List.of(new ParseNode(Symbol.VarX),
                                            new ParseNode(Symbol.VarZ))))),
                    new ParseNode(Symbol.Const1),
                    new ParseNode(Symbol.Const2)));
    public static final Symbol[] iteOrTestPostOrder = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarZ,
            Symbol.VarX, Symbol.Eq, Symbol.VarY, Symbol.VarX, Symbol.Eq, Symbol.Or, Symbol.Ite };
    public static final ExprNode iteOrTestExprAst = new IteNode(
            new OrNode(
                    new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)),
                    new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarZ))),
            new ExprConstNode(1),
            new ExprConstNode(2));
    public static final Example[] iteOrTestExamples = new Example[] {
            new Example(new Environment(1, 1, 1), 1),
            new Example(new Environment(1, 1, 2), 1),
            new Example(new Environment(1, 2, 1), 1),
            new Example(new Environment(1, 2, 2), 2) };
    public static final int iteOrTestParseNodeSize = iteOrTestPostOrder.length;

    public static final ParseNode iteNotTestParseTree = new ParseNode(Symbol.Ite,
            List.of(new ParseNode(Symbol.Not,
                    List.of(new ParseNode(Symbol.Eq,
                            List.of(new ParseNode(Symbol.VarX),
                                    new ParseNode(Symbol.VarY))))),
                    new ParseNode(Symbol.Const1),
                    new ParseNode(Symbol.Const2)));
    public static final Symbol[] iteNotTestPostOrder = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarY,
            Symbol.VarX, Symbol.Eq, Symbol.Not, Symbol.Ite };
    public static final ExprNode iteNotTestExprAst = new IteNode(
            new NotNode(new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY))),
            new ExprConstNode(1),
            new ExprConstNode(2));
    public static final Example[] iteNotTestExamples = new Example[] {
            new Example(new Environment(10, 15, 20), 1),
            new Example(new Environment(15, 15, 20), 2) };
    public static final int iteNotTestParseNodeSize = iteNotTestPostOrder.length;

    public static final ParseNode compositionTestParseTree = new ParseNode(Symbol.Ite,
            List.of(new ParseNode(Symbol.Lt,
                    List.of(new ParseNode(Symbol.VarX),
                            new ParseNode(Symbol.Const3))),
                    new ParseNode(Symbol.Add,
                            List.of(new ParseNode(Symbol.VarY),
                                    new ParseNode(Symbol.VarZ))),
                    new ParseNode(Symbol.Multiply,
                            List.of(new ParseNode(Symbol.VarY),
                                    new ParseNode(Symbol.VarZ)))));
    public static final Symbol[] compositionTestPostOrder = new Symbol[] { Symbol.VarZ, Symbol.VarY, Symbol.Multiply,
            Symbol.VarZ, Symbol.VarY, Symbol.Add, Symbol.Const3, Symbol.VarX, Symbol.Lt, Symbol.Ite };
    public static final ExprNode compositionTestExprAst = new IteNode(
            new LtNode(new VariableNode(Symbol.VarX), new ExprConstNode(3)),
            new AddNode(new VariableNode(Symbol.VarY), new VariableNode(Symbol.VarZ)),
            new MultiplyNode(new VariableNode(Symbol.VarY), new VariableNode(Symbol.VarZ)));
    public static final Example[] compositionTestExamples = new Example[] {
            new Example(new Environment(10, 15, 20), 300),
            new Example(new Environment(0, 15, 20), 35) };
    public static final int compositionTestParseNodeSize = compositionTestPostOrder.length;

    public static final Symbol[] ltTestBoolPostOrder = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Lt };
    public static final BoolNode ltTestBoolAst = new LtNode(new VariableNode(Symbol.VarX),
            new VariableNode(Symbol.VarY));
    public static final Example[] ltTestIncluded = new Example[] { new Example(new Environment(10, 15, 20), 1) };
    public static final Example[] ltTestExcluded = new Example[] { new Example(new Environment(15, 15, 20), 2),
            new Example(new Environment(20, 15, 20), 2) };
    public static final int ltTestBoolParseNodeSize = ltTestBoolPostOrder.length;

    public static final Symbol[] eqTestBoolPostOrder = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Eq };
    public static final BoolNode eqTestBoolAst = new EqNode(new VariableNode(Symbol.VarX),
            new VariableNode(Symbol.VarY));
    public static final Example[] eqTestIncluded = new Example[] { new Example(new Environment(15, 15, 20), 1) };
    public static final Example[] eqTestExcluded = new Example[] { new Example(new Environment(10, 15, 20), 2),
            new Example(new Environment(20, 15, 20), 2) };
    public static final int eqTestBoolParseNodeSize = eqTestBoolPostOrder.length;

    public static final Symbol[] andTestBoolPostOrder = new Symbol[] { Symbol.VarZ, Symbol.VarX, Symbol.Eq, Symbol.VarY,
            Symbol.VarX, Symbol.Eq, Symbol.And };
    public static final BoolNode andTestBoolAst = new AndNode(
            new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)),
            new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarZ)));
    public static final Example[] andTestIncluded = new Example[] { new Example(new Environment(1, 1, 1), 1) };
    public static final Example[] andTestExcluded = new Example[] { new Example(new Environment(1, 1, 2), 2),
            new Example(new Environment(1, 2, 1), 2), new Example(new Environment(1, 2, 2), 2) };
    public static final int andTestBoolParseNodeSize = andTestBoolPostOrder.length;

    public static final Symbol[] orTestBoolPostOrder = new Symbol[] { Symbol.VarZ, Symbol.VarX, Symbol.Eq, Symbol.VarY,
            Symbol.VarX, Symbol.Eq, Symbol.Or };
    public static final BoolNode orTestBoolAst = new OrNode(
            new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)),
            new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarZ)));
    public static final Example[] orTestIncluded = new Example[] { new Example(new Environment(1, 1, 1), 1),
            new Example(new Environment(1, 1, 2), 1), new Example(new Environment(1, 2, 1), 1) };
    public static final Example[] orTestExcluded = new Example[] { new Example(new Environment(1, 2, 2), 2) };
    public static final int orTestBoolParseNodeSize = orTestBoolPostOrder.length;

    public static final Symbol[] notTestBoolPostOrder = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Eq,
            Symbol.Not };
    public static final BoolNode notTestBoolAst = new NotNode(
            new EqNode(new VariableNode(Symbol.VarX), new VariableNode(Symbol.VarY)));
    public static final Example[] notTestIncluded = new Example[] { new Example(new Environment(10, 15, 20), 1) };
    public static final Example[] notTestExcluded = new Example[] { new Example(new Environment(15, 15, 20), 2) };
    public static final int notTestBoolParseNodeSize = notTestBoolPostOrder.length;

}
