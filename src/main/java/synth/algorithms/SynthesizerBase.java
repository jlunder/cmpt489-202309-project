package synth.algorithms;

import synth.core.Environment;
import synth.core.Example;
import synth.core.ParseNode;
import synth.core.Program;
import synth.core.Synthesizer;
import synth.dsl.*;

import java.util.*;
import java.util.function.*;

public abstract class SynthesizerBase implements Synthesizer {

    protected static boolean validate(List<Example> examples, Program program) {
        return validate(examples, env -> Semantics.evaluate(program, env));
    }

    protected static boolean validate(List<Example> examples, ParseNode program) {
        return validate(examples, env -> Semantics.evaluate(program, env));
    }

    protected static boolean validate(List<Example> examples, List<Symbol> program) {
        return validate(examples, env -> Semantics.evaluate(program.iterator(), env));
    }

    protected static boolean validate(List<Example> examples, Function<Environment, Integer> evaluator) {
        // Run the program in each interpreter env representing a particular example,
        // and check whether the output is as expected
        for (Example ex : examples) {
            if (evaluator.apply(ex.input()) != ex.output()) {
                // This example produces incorrect output
                return false;
            }
        }
        // No examples failed, we have a winner!
        return true;
    }

}
