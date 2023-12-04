package synth.algorithms.classify;

import java.util.List;

import synth.algorithms.ast.*;
import synth.core.Example;

public class Discriminator {
    Classification classification;
    BoolNode condition;
    // complexity metric?

    public Discriminator(BoolNode condition, List<Example> examples) {
        this.classification = Classification.makeFromCondition(condition, examples);
        this.condition = condition;
    }
}
