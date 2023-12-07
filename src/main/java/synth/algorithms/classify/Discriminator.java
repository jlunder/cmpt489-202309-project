package synth.algorithms.classify;

import java.util.Collection;

import synth.algorithms.representation.BoolRepresentation;
import synth.core.Example;

public class Discriminator {
    private Classification classification;
    private BoolRepresentation condition;
    // complexity metric?

    public Classification classification() {
        return classification;
    }

    public BoolRepresentation condition() {
        return condition;
    }

    public Discriminator(BoolRepresentation condition, Collection<Example> examples) {
        this.classification = Classification.makeFromCondition(condition, examples);
        this.condition = condition;
    }
}
