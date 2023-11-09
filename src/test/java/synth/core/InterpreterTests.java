package synth.core;

import org.junit.Assert;
import org.junit.Test;
import synth.cfg.Symbol;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, Integer> buildEnvironment() {
        Map<String, Integer> env = new HashMap<>();
        env.put("x", 10);
        env.put("y", 15);
        env.put("z", 20);
        return env;
    }

    @Test
    public void testInterpreter1() {
        // Add(x, y)
        Program program = new Program(
                new ASTNode(Symbol.Add,
                        List.of(
                                new ASTNode(Symbol.VarX, Collections.emptyList()),
                                new ASTNode(Symbol.VarY, Collections.emptyList()))));
        int result = Interpreter.evaluate(program, buildEnvironment());
        Assert.assertEquals(25, result);
    }

    @Test
    public void testInterpreter2() {
        // Multiply(z, 2)
        Program program = new Program(
                new ASTNode(Symbol.Multiply,
                        List.of(
                                new ASTNode(Symbol.VarZ, Collections.emptyList()),
                                new ASTNode(Symbol.Const2, Collections.emptyList()))));
        int result = Interpreter.evaluate(program, buildEnvironment());
        Assert.assertEquals(40, result);
    }

    @Test
    public void testInterpreter3() {
        // Ite(Lt(x, 3), Add(y, z), Multiply(y, z))
        Program program = new Program(
                new ASTNode(Symbol.Ite,
                        List.of(
                                new ASTNode(Symbol.Lt,
                                        List.of(
                                                new ASTNode(Symbol.VarX, Collections.emptyList()),
                                                new ASTNode(Symbol.Const3, Collections.emptyList()))),
                                new ASTNode(Symbol.Add,
                                        List.of(
                                                new ASTNode(Symbol.VarY, Collections.emptyList()),
                                                new ASTNode(Symbol.VarZ, Collections.emptyList()))),
                                new ASTNode(Symbol.Multiply,
                                        List.of(
                                                new ASTNode(Symbol.VarY, Collections.emptyList()),
                                                new ASTNode(Symbol.VarZ, Collections.emptyList()))))));
        int result = Interpreter.evaluate(program, buildEnvironment());
        Assert.assertEquals(300, result);
    }
}
