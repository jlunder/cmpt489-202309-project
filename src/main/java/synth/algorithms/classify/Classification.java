package synth.algorithms.classify;

import java.lang.ref.WeakReference;
import java.util.*;

import synth.algorithms.representation.*;
import synth.core.Example;

public class Classification {
    private HashSet<Example> included;
    private HashSet<Example> excluded;
    private WeakReference<Classification> invertedCache;
    private int cachedHashCode;

    public static Classification makeFromExamples(ExprRepresentation expr, Collection<Example> examples) {
        var included = new HashSet<Example>(examples.size());
        var excluded = new HashSet<Example>(examples.size());
        for (var ex : examples) {
            if (expr.evalExpr(ex.input()) == ex.output()) {
                included.add(ex);
            } else {
                excluded.add(ex);
            }
        }

        return new Classification(included, excluded, null);
    }

    public static Classification makeFromCondition(BoolRepresentation condition, Collection<Example> examples) {
        var included = new HashSet<Example>(examples.size());
        var excluded = new HashSet<Example>(examples.size());
        for (var ex : examples) {
            if (condition.evalBool(ex.input())) {
                included.add(ex);
            } else {
                excluded.add(ex);
            }
        }

        return new Classification(included, excluded, null);
    }

    protected Classification(HashSet<Example> included, HashSet<Example> excluded, Classification inverted) {
        // An empty classification will surely lead to undefined behaviour...
        //assert !(included.isEmpty() && excluded.isEmpty());
        this.included = included;
        this.excluded = excluded;
        this.cachedHashCode = included.hashCode() * 1093742879 + excluded.hashCode();
        if (inverted != null) {
            this.invertedCache = new WeakReference<Classification>(inverted);
        }
    }

    public Set<Example> included() {
        return included;
    }

    public Set<Example> excluded() {
        return excluded;
    }

    public Classification inverted() {
        Classification cached = invertedCache != null ? invertedCache.get() : null;
        if (cached != null) {
            return cached;
        }
        cached = new Classification(excluded, included, this);
        invertedCache = new WeakReference<Classification>(cached);
        return cached;
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

    public boolean equalsInverted(Classification other) {
        if (other == this || other == null || other.cachedHashCode == cachedHashCode) {
            return false;
        }
        return Objects.equals(other.included, excluded) && Objects.equals(other.excluded, included);
    }
}
