package synth.core;

import org.junit.Assert;
import org.junit.Test;

import synth.dsl.*;

import java.util.*;

/**
 * Tests for interpreters.
 * Note that there are only three variables x, y, z and three constants 1, 2, 3.
 */
public class InterpreterTests {

    /**
     * Build an environment where x=10, y=15, z=20
     *
     * @return the environment map
     */
    private Environment buildEnvironment() {
        return new Environment(10, 15, 20);
    }

    @Test
    public void testInterpreter1() {
        // Add(x, y)
        Program program = new Program(
                new ParseNode(Symbol.Add,
                        List.of(
                                new ParseNode(Symbol.VarX, Collections.emptyList()),
                                new ParseNode(Symbol.VarY, Collections.emptyList()))));
        int result = Semantics.evaluate(program, buildEnvironment());
        Assert.assertEquals(25, result);
    }

    @Test
    public void testInterpreter2() {
        // Multiply(z, 2)
        Program program = new Program(
                new ParseNode(Symbol.Multiply,
                        List.of(
                                new ParseNode(Symbol.VarZ, Collections.emptyList()),
                                new ParseNode(Symbol.Const2, Collections.emptyList()))));
        int result = Semantics.evaluate(program, buildEnvironment());
        Assert.assertEquals(40, result);
    }

    @Test
    public void testInterpreter3() {
        // Ite(Lt(x, 3), Add(y, z), Multiply(y, z))
        Program program = new Program(
                new ParseNode(Symbol.Ite,
                        List.of(
                                new ParseNode(Symbol.Lt,
                                        List.of(
                                                new ParseNode(Symbol.VarX, Collections.emptyList()),
                                                new ParseNode(Symbol.Const3, Collections.emptyList()))),
                                new ParseNode(Symbol.Add,
                                        List.of(
                                                new ParseNode(Symbol.VarY, Collections.emptyList()),
                                                new ParseNode(Symbol.VarZ, Collections.emptyList()))),
                                new ParseNode(Symbol.Multiply,
                                        List.of(
                                                new ParseNode(Symbol.VarY, Collections.emptyList()),
                                                new ParseNode(Symbol.VarZ, Collections.emptyList()))))));
        int result = Semantics.evaluate(program, buildEnvironment());
        Assert.assertEquals(300, result);
    }
}
