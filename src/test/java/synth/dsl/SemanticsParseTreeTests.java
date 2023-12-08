package synth.dsl;

import org.junit.*;

import synth.core.*;

import java.util.*;

public class SemanticsParseTreeTests {
    @Test
    public void testParseTreeConst1() {
        Program program = new Program(new ParseNode(Symbol.Const1));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeConst2() {
        Program program = new Program(new ParseNode(Symbol.Const2));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeConst3() {
        Program program = new Program(new ParseNode(Symbol.Const3));
        Assert.assertEquals(3, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeVarX() {
        Program program = new Program(new ParseNode(Symbol.VarX));
        Assert.assertEquals(10, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeVarY() {
        Program program = new Program(new ParseNode(Symbol.VarY));
        Assert.assertEquals(15, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeVarZ() {
        Program program = new Program(new ParseNode(Symbol.VarZ));
        Assert.assertEquals(20, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeAdd() {
        Program program = new Program(
                new ParseNode(Symbol.Add,
                        List.of(new ParseNode(Symbol.VarX),
                                new ParseNode(Symbol.VarY))));
        Assert.assertEquals(25, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeMultiply() {
        Program program = new Program(
                new ParseNode(Symbol.Multiply,
                        List.of(new ParseNode(Symbol.VarZ),
                                new ParseNode(Symbol.Const2))));
        Assert.assertEquals(40, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeLt() {
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(new ParseNode(Symbol.Lt,
                                List.of(new ParseNode(Symbol.VarX),
                                        new ParseNode(Symbol.VarY))),
                                new ParseNode(Symbol.Const1),
                                new ParseNode(Symbol.Const2))));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(10, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(15, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(20, 15, 20)));
    }

    @Test
    public void testParseTreeEq() {
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(new ParseNode(Symbol.Eq,
                                List.of(new ParseNode(Symbol.VarX),
                                        new ParseNode(Symbol.VarY))),
                                new ParseNode(Symbol.Const1),
                                new ParseNode(Symbol.Const2))));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(10, 15, 20)));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(15, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testParseTreeAnd() {
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(new ParseNode(Symbol.And,
                                List.of(new ParseNode(Symbol.Eq,
                                        List.of(new ParseNode(Symbol.VarX),
                                                new ParseNode(Symbol.VarY))),
                                        new ParseNode(Symbol.Eq,
                                                List.of(new ParseNode(Symbol.VarX),
                                                        new ParseNode(Symbol.VarZ))))),
                                new ParseNode(Symbol.Const1),
                                new ParseNode(Symbol.Const2))));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(1, 1, 1)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(1, 1, 2)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(1, 2, 1)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(1, 2, 2)));
    }

    @Test
    public void testParseTreeOr() {
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(new ParseNode(Symbol.Or,
                                List.of(new ParseNode(Symbol.Eq,
                                        List.of(new ParseNode(Symbol.VarX),
                                                new ParseNode(Symbol.VarY))),
                                        new ParseNode(Symbol.Eq,
                                                List.of(new ParseNode(Symbol.VarX),
                                                        new ParseNode(Symbol.VarZ))))),
                                new ParseNode(Symbol.Const1),
                                new ParseNode(Symbol.Const2))));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(1, 1, 1)));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(1, 1, 2)));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(1, 2, 1)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(1, 2, 2)));
    }

    @Test
    public void testParseTreeNot() {
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(new ParseNode(Symbol.Not,
                                List.of(new ParseNode(Symbol.Eq,
                                        List.of(new ParseNode(Symbol.VarX),
                                                new ParseNode(Symbol.VarY))))),
                                new ParseNode(Symbol.Const1),
                                new ParseNode(Symbol.Const2))));
        Assert.assertEquals(1, Semantics.evaluate(program, new Environment(10, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluate(program, new Environment(15, 15, 20)));
    }

    @Test
    public void testComposition() {
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(new ParseNode(Symbol.Lt,
                                List.of(new ParseNode(Symbol.VarX),
                                        new ParseNode(Symbol.Const3))),
                                new ParseNode(Symbol.Add,
                                        List.of(new ParseNode(Symbol.VarY),
                                                new ParseNode(Symbol.VarZ))),
                                new ParseNode(Symbol.Multiply,
                                        List.of(new ParseNode(Symbol.VarY),
                                                new ParseNode(Symbol.VarZ))))));
        Assert.assertEquals(300, Semantics.evaluate(program, new Environment(10, 15, 20)));
        Assert.assertEquals(35, Semantics.evaluate(program, new Environment(0, 15, 20)));
    }
}
