package synth.algorithms.classify;

import java.util.Collection;

import synth.algorithms.ast.*;
import synth.core.Example;

public class Discriminator {
    private Classification classification;
    private BoolNode condition;
    // complexity metric?

    public Classification classification() {
        return classification;
    }

    public BoolNode condition() {
        return condition;
    }

    public Discriminator(BoolNode condition, Collection<Example> examples) {
        this.classification = Classification.makeFromCondition(condition, examples);
        this.condition = condition;
    }
}
