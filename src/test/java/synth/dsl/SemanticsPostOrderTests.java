package synth.dsl;

import org.junit.*;

import synth.core.*;
import synth.util.TestData;
import synth.util.Tests;

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
    public void testPostOrderConst1() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.const1TestPostOrder, TestData.const1TestExamples);
    }

    @Test
    public void testPostOrderConst2() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.const2TestPostOrder, TestData.const2TestExamples);
    }

    @Test
    public void testPostOrderConst3() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.const3TestPostOrder, TestData.const3TestExamples);
    }

    @Test
    public void testPostOrderVarX() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.varXTestPostOrder, TestData.varXTestExamples);
    }

    @Test
    public void testPostOrderVarY() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.varYTestPostOrder, TestData.varYTestExamples);
    }

    @Test
    public void testPostOrderVarZ() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.varZTestPostOrder, TestData.varZTestExamples);
    }

    @Test
    public void testPostOrderAdd() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.addTestPostOrder, TestData.addTestExamples);
    }

    @Test
    public void testPostOrderMultiply() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.multiplyTestPostOrder, TestData.multiplyTestExamples);
    }

    @Test
    public void testPostOrderIteLt() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.iteLtTestPostOrder, TestData.iteLtTestExamples);
    }

    @Test
    public void testPostOrderIteEq() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.iteEqTestPostOrder, TestData.iteEqTestExamples);
    }

    @Test
    public void testPostOrderIteAnd() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.iteAndTestPostOrder, TestData.iteAndTestExamples);
    }

    @Test
    public void testPostOrderIteOr() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.iteOrTestPostOrder, TestData.iteOrTestExamples);
    }

    @Test
    public void testPostOrderIteNot() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.iteNotTestPostOrder, TestData.iteNotTestExamples);
    }

    @Test
    public void testPostOrderComposition() {
        Tests.assertExprPostOrderSatisfiesExamples(TestData.compositionTestPostOrder,
                TestData.compositionTestExamples);
    }

    @Test
    public void testPostOrderLt() {
        Tests.assertBoolPostOrderSatisfiesExamples(TestData.ltTestBoolPostOrder, TestData.ltTestIncluded,
                TestData.ltTestExcluded);
    }

    @Test
    public void testPostOrderEq() {
        Tests.assertBoolPostOrderSatisfiesExamples(TestData.eqTestBoolPostOrder, TestData.eqTestIncluded,
                TestData.eqTestExcluded);
    }

    @Test
    public void testPostOrderAnd() {
        Tests.assertBoolPostOrderSatisfiesExamples(TestData.andTestBoolPostOrder, TestData.andTestIncluded,
                TestData.andTestExcluded);
    }

    @Test
    public void testPostOrderOr() {
        Tests.assertBoolPostOrderSatisfiesExamples(TestData.orTestBoolPostOrder, TestData.orTestIncluded,
                TestData.orTestExcluded);
    }

    @Test
    public void testPostOrderNot() {
        Tests.assertBoolPostOrderSatisfiesExamples(TestData.notTestBoolPostOrder, TestData.notTestIncluded,
                TestData.notTestExcluded);
    }

    @Test
    public void testPostOrderConst1Size() {
        Assert.assertEquals(TestData.const1TestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.const1TestPostOrder));
    }

    @Test
    public void testPostOrderConst2Size() {
        Assert.assertEquals(TestData.const2TestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.const2TestPostOrder));
    }

    @Test
    public void testPostOrderConst3Size() {
        Assert.assertEquals(TestData.const3TestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.const3TestPostOrder));
    }

    @Test
    public void testPostOrderVarXSize() {
        Assert.assertEquals(TestData.varXTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.varXTestPostOrder));
    }

    @Test
    public void testPostOrderVarYSize() {
        Assert.assertEquals(TestData.varYTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.varYTestPostOrder));
    }

    @Test
    public void testPostOrderVarZSize() {
        Assert.assertEquals(TestData.varZTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.varZTestPostOrder));
    }

    @Test
    public void testPostOrderAddSize() {
        Assert.assertEquals(TestData.addTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.addTestPostOrder));
    }

    @Test
    public void testPostOrderMultiplySize() {
        Assert.assertEquals(TestData.multiplyTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.multiplyTestPostOrder));
    }

    @Test
    public void testPostOrderLtSize() {
        Assert.assertEquals(TestData.iteLtTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.iteLtTestPostOrder));
    }

    @Test
    public void testPostOrderEqSize() {
        Assert.assertEquals(TestData.iteEqTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.iteEqTestPostOrder));
    }

    @Test
    public void testPostOrderAndSize() {
        Assert.assertEquals(TestData.iteAndTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.iteAndTestPostOrder));
    }

    @Test
    public void testPostOrderOrSize() {
        Assert.assertEquals(TestData.iteOrTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.iteOrTestPostOrder));
    }

    @Test
    public void testPostOrderNotSize() {
        Assert.assertEquals(TestData.iteNotTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.iteNotTestPostOrder));
    }

    @Test
    public void testPostOrderCompositionSize() {
        Assert.assertEquals(TestData.compositionTestParseNodeSize,
                Semantics.measureExprPostOrderSize(TestData.compositionTestPostOrder));
    }
}
