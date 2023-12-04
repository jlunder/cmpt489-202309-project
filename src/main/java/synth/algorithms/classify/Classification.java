package synth.algorithms.classify;

import java.util.*;

import synth.algorithms.ast.*;
import synth.core.*;

public class Classification {
    private Set<Example> included;
    private Set<Example> excluded;
    private int cachedHashCode;

    public static Classification makeFromCondition(BoolNode condition, Collection<Example> examples) {
        var included = new HashSet<Example>(examples.size());
        var excluded = new HashSet<Example>(examples.size());
        for (var ex : examples) {
            if (condition.evalBool(ex.input())) {
                included.add(ex);
            } else {
                excluded.add(ex);
            }
        }

        return new Classification(included, excluded);
    }

    public Classification(Set<Example> included, Set<Example> excluded) {
        this.included = included;
        this.excluded = excluded;
        this.cachedHashCode = included.hashCode() * 1093742879 + excluded.hashCode();
    }

    public Set<Example> included() {
        return included;
    }

    public Set<Example> excluded() {
        return excluded;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof Classification)) {
            return false;
        }
        Classification cObj = (Classification) obj;
        if (cObj.cachedHashCode != cachedHashCode) {
            return false;
        }
        return Objects.equals(cObj.included, included) && Objects.equals(cObj.excluded, excluded);
    }
}
