package synth.algorithms.classify;

import java.util.Collection;

import synth.algorithms.ast.BoolNode;
import synth.algorithms.representation.BoolRepresentation;
import synth.core.*;

public class Discriminator implements BoolRepresentation {
    private Classification classification;
    private BoolRepresentation condition;
    // complexity metric?

    public Classification classification() {
        return classification;
    }

    public BoolRepresentation condition() {
        return condition;
    }

    public Discriminator(BoolRepresentation condition, Collection<Environment> examples) {
        this.classification = Classification.makeFromCondition(condition, examples);
        this.condition = condition;
    }

    @Override
    public BoolNode reifyAsBoolAst() {
        return condition.reifyAsBoolAst();
    }

    @Override
    public ParseNode reifyAsBoolParse() {
        return condition.reifyAsBoolParse();
    }

    @Override
    public boolean evalBool(Environment env) {
        if (classification.included().contains(env)) {
            return true;
        } else if(classification.excluded().contains(env)) {
            return false;
        } else {
            return condition.evalBool(env);
        }
    }
}
