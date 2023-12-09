package synth.algorithms.ast;

import synth.util.*;

import org.junit.*;

public class AstEvalTests {
    @Test
    public void testExprAstConst1() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.const1TestExprAst, TestData.const1TestExamples);
    }

    @Test
    public void testExprAstConst2() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.const2TestExprAst, TestData.const2TestExamples);
    }

    @Test
    public void testExprAstConst3() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.const3TestExprAst, TestData.const3TestExamples);
    }

    @Test
    public void testExprAstVarX() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.varXTestExprAst, TestData.varXTestExamples);
    }

    @Test
    public void testExprAstVarY() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.varYTestExprAst, TestData.varYTestExamples);
    }

    @Test
    public void testExprAstVarZ() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.varZTestExprAst, TestData.varZTestExamples);
    }

    @Test
    public void testExprAstAdd() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.addTestExprAst, TestData.addTestExamples);
    }

    @Test
    public void testExprAstMultiply() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.multiplyTestExprAst, TestData.multiplyTestExamples);
    }

    @Test
    public void testExprAstLt() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.iteLtTestExprAst, TestData.iteLtTestExamples);
    }

    @Test
    public void testExprAstEq() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.iteEqTestExprAst, TestData.iteEqTestExamples);
    }

    @Test
    public void testExprAstAnd() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.iteAndTestExprAst, TestData.iteAndTestExamples);
    }

    @Test
    public void testExprAstOr() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.iteOrTestExprAst, TestData.iteOrTestExamples);
    }

    @Test
    public void testExprAstNot() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.iteNotTestExprAst, TestData.iteNotTestExamples);
    }

    @Test
    public void testExprAstComposition() {
        Tests.assertExprRepresentationSatisfiesExamples(TestData.compositionTestExprAst,
                TestData.compositionTestExamples);
    }

}
