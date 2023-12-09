package synth.dsl;

import org.junit.*;

import synth.util.TestData;
import synth.util.Tests;

public class SemanticsParseTreeTests {
    @Test
    public void testParseTreeConst1() {
        Tests.assertParseTreeSatisfiesExamples(TestData.const1TestParseTree, TestData.const1TestExamples);
    }

    @Test
    public void testParseTreeConst2() {
        Tests.assertParseTreeSatisfiesExamples(TestData.const2TestParseTree, TestData.const2TestExamples);
    }

    @Test
    public void testParseTreeConst3() {
        Tests.assertParseTreeSatisfiesExamples(TestData.const3TestParseTree, TestData.const3TestExamples);
    }

    @Test
    public void testParseTreeVarX() {
        Tests.assertParseTreeSatisfiesExamples(TestData.varXTestParseTree, TestData.varXTestExamples);
    }

    @Test
    public void testParseTreeVarY() {
        Tests.assertParseTreeSatisfiesExamples(TestData.varYTestParseTree, TestData.varYTestExamples);
    }

    @Test
    public void testParseTreeVarZ() {
        Tests.assertParseTreeSatisfiesExamples(TestData.varZTestParseTree, TestData.varZTestExamples);
    }

    @Test
    public void testParseTreeAdd() {
        Tests.assertParseTreeSatisfiesExamples(TestData.addTestParseTree, TestData.addTestExamples);
    }

    @Test
    public void testParseTreeMultiply() {
        Tests.assertParseTreeSatisfiesExamples(TestData.multiplyTestParseTree, TestData.multiplyTestExamples);
    }

    @Test
    public void testParseTreeLt() {
        Tests.assertParseTreeSatisfiesExamples(TestData.iteLtTestParseTree, TestData.iteLtTestExamples);
    }

    @Test
    public void testParseTreeEq() {
        Tests.assertParseTreeSatisfiesExamples(TestData.iteEqTestParseTree, TestData.iteEqTestExamples);
    }

    @Test
    public void testParseTreeAnd() {
        Tests.assertParseTreeSatisfiesExamples(TestData.iteAndTestParseTree, TestData.iteAndTestExamples);
    }

    @Test
    public void testParseTreeOr() {
        Tests.assertParseTreeSatisfiesExamples(TestData.iteOrTestParseTree, TestData.iteOrTestExamples);
    }

    @Test
    public void testParseTreeNot() {
        Tests.assertParseTreeSatisfiesExamples(TestData.iteNotTestParseTree, TestData.iteNotTestExamples);
    }

    @Test
    public void testParseTreeComposition() {
        Tests.assertParseTreeSatisfiesExamples(TestData.compositionTestParseTree,
                TestData.compositionTestExamples);
    }

    @Test
    public void testParseTreeConst1Size() {
        Assert.assertEquals(TestData.const1TestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.const1TestParseTree));
    }

    @Test
    public void testParseTreeConst2Size() {
        Assert.assertEquals(TestData.const2TestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.const2TestParseTree));
    }

    @Test
    public void testParseTreeConst3Size() {
        Assert.assertEquals(TestData.const3TestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.const3TestParseTree));
    }

    @Test
    public void testParseTreeVarXSize() {
        Assert.assertEquals(TestData.varXTestParseNodeSize, Semantics.measureParseTreeSize(TestData.varXTestParseTree));
    }

    @Test
    public void testParseTreeVarYSize() {
        Assert.assertEquals(TestData.varYTestParseNodeSize, Semantics.measureParseTreeSize(TestData.varYTestParseTree));
    }

    @Test
    public void testParseTreeVarZSize() {
        Assert.assertEquals(TestData.varZTestParseNodeSize, Semantics.measureParseTreeSize(TestData.varZTestParseTree));
    }

    @Test
    public void testParseTreeAddSize() {
        Assert.assertEquals(TestData.addTestParseNodeSize, Semantics.measureParseTreeSize(TestData.addTestParseTree));
    }

    @Test
    public void testParseTreeMultiplySize() {
        Assert.assertEquals(TestData.multiplyTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.multiplyTestParseTree));
    }

    @Test
    public void testParseTreeLtSize() {
        Assert.assertEquals(TestData.iteLtTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.iteLtTestParseTree));
    }

    @Test
    public void testParseTreeEqSize() {
        Assert.assertEquals(TestData.iteEqTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.iteEqTestParseTree));
    }

    @Test
    public void testParseTreeAndSize() {
        Assert.assertEquals(TestData.iteAndTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.iteAndTestParseTree));
    }

    @Test
    public void testParseTreeOrSize() {
        Assert.assertEquals(TestData.iteOrTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.iteOrTestParseTree));
    }

    @Test
    public void testParseTreeNotSize() {
        Assert.assertEquals(TestData.iteNotTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.iteNotTestParseTree));
    }

    @Test
    public void testParseTreeCompositionSize() {
        Assert.assertEquals(TestData.compositionTestParseNodeSize,
                Semantics.measureParseTreeSize(TestData.compositionTestParseTree));
    }
}
