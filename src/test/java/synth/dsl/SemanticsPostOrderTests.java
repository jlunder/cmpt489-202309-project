package synth.dsl;

import org.junit.*;

import synth.core.*;

public class SemanticsPostOrderTests {
    @Test
    public void testExprPostOrderEmpty() {
        Symbol[] program = new Symbol[] {};
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderExtra() {
        Symbol[] program = new Symbol[] { Symbol.Const1, Symbol.VarX };
        Assert.assertEquals(10, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderConst1() {
        Symbol[] program = new Symbol[] { Symbol.Const1 };
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderConst2() {
        Symbol[] program = new Symbol[] { Symbol.Const2 };
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderConst3() {
        Symbol[] program = new Symbol[] { Symbol.Const3 };
        Assert.assertEquals(3, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderVarX() {
        Symbol[] program = new Symbol[] { Symbol.VarX };
        Assert.assertEquals(10, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderVarY() {
        Symbol[] program = new Symbol[] { Symbol.VarY };
        Assert.assertEquals(15, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderVarZ() {
        Symbol[] program = new Symbol[] { Symbol.VarZ };
        Assert.assertEquals(20, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderAdd() {
        Symbol[] program = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Add };
        Assert.assertEquals(25, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderMultiply() {
        Symbol[] program = new Symbol[] { Symbol.Const2, Symbol.VarZ, Symbol.Multiply };
        Assert.assertEquals(40, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderLt() {
        Symbol[] program = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarY, Symbol.VarX, Symbol.Lt,
                Symbol.Ite };
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(15, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(20, 15, 20)));
    }

    @Test
    public void testExprPostOrderEq() {
        Symbol[] program = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarY, Symbol.VarX, Symbol.Eq,
                Symbol.Ite };
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(15, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testExprPostOrderAnd() {
        Symbol[] program = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarZ, Symbol.VarX, Symbol.Eq,
                Symbol.VarY, Symbol.VarX, Symbol.Eq, Symbol.And, Symbol.Ite };
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(1, 1, 1)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(1, 1, 2)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(1, 2, 1)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(1, 2, 2)));
    }

    @Test
    public void testExprPostOrderOr() {
        Symbol[] program = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarZ, Symbol.VarX, Symbol.Eq,
                Symbol.VarY, Symbol.VarX, Symbol.Eq, Symbol.Or, Symbol.Ite };
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(1, 1, 1)));
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(1, 1, 2)));
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(1, 2, 1)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(1, 2, 2)));
    }

    @Test
    public void testExprPostOrderNot() {
        Symbol[] program = new Symbol[] { Symbol.Const2, Symbol.Const1, Symbol.VarY, Symbol.VarX, Symbol.Eq, Symbol.Not,
                Symbol.Ite };
        Assert.assertEquals(1, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(2, Semantics.evaluateExprPostOrder(program, new Environment(15, 15, 20)));
    }

    @Test
    public void testBoolPostOrderLt() {
        Symbol[] program = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Lt };
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(15, 15, 20)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(20, 15, 20)));
    }

    @Test
    public void testBoolPostOrderEq() {
        Symbol[] program = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Eq };
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(15, 15, 20)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(10, 15, 20)));
    }

    @Test
    public void testBoolPostOrderAnd() {
        Symbol[] program = new Symbol[] { Symbol.VarZ, Symbol.VarX, Symbol.Eq, Symbol.VarY, Symbol.VarX, Symbol.Eq,
                Symbol.And };
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(1, 1, 1)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(1, 1, 2)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(1, 2, 1)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(1, 2, 2)));
    }

    @Test
    public void testBoolPostOrderOr() {
        Symbol[] program = new Symbol[] { Symbol.VarZ, Symbol.VarX, Symbol.Eq, Symbol.VarY, Symbol.VarX, Symbol.Eq,
                Symbol.Or };
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(1, 1, 1)));
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(1, 1, 2)));
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(1, 2, 1)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(1, 2, 2)));
    }

    @Test
    public void testBoolPostOrderNot() {
        Symbol[] program = new Symbol[] { Symbol.VarY, Symbol.VarX, Symbol.Eq, Symbol.Not };
        Assert.assertEquals(true, Semantics.evaluateBoolPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(false, Semantics.evaluateBoolPostOrder(program, new Environment(15, 15, 20)));
    }

    @Test
    public void testComposition() {
        Symbol[] program = new Symbol[] { Symbol.VarZ, Symbol.VarY, Symbol.Multiply, Symbol.VarZ, Symbol.VarY,
                Symbol.Add, Symbol.Const3, Symbol.VarX, Symbol.Lt, Symbol.Ite };
        Assert.assertEquals(300, Semantics.evaluateExprPostOrder(program, new Environment(10, 15, 20)));
        Assert.assertEquals(35, Semantics.evaluateExprPostOrder(program, new Environment(0, 15, 20)));
    }
}
