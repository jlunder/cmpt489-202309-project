package synth.algorithms.lia;

import java.util.*;

import synth.algorithms.ast.*;
import synth.algorithms.representation.ExprRepresentation;
import synth.core.*;

public class LinearSolution implements Comparable<LinearSolution>, ExprRepresentation {
    private final Map<Term, Integer> coefficientMap;

    // A packed map of the terms and their values; negative values are (negated)
    // term indexes, positive values are coefficients. Sequential coefficients
    // correspond to sequential terms.
    private final int[] signature;

    public Map<Term, Integer> coefficients() {
        return coefficientMap;
    }

    public LinearSolution(Map<Term, Integer> coefficientMap) {
        this.coefficientMap = coefficientMap;

        var accum = new ArrayList<Integer>(coefficientMap.size() * 2);
        var terms = new ArrayList<Term>(coefficientMap.keySet());
        Collections.sort(terms);
        int expected = 0;
        for (var t : terms) {
            var i = t.index();
            // Only write the term index if it's not sequential
            if (i != expected) {
                accum.add(-i);
                expected = i;
            }
            accum.add(coefficientMap.get(t));
            ++expected;
        }
        signature = new int[accum.size()];
        for (int i = 0; i < accum.size(); ++i) {
            signature[i] = accum.get(i);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this) || (obj instanceof LinearSolution)
                && (Arrays.equals(signature, ((LinearSolution) obj).signature));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(signature);
    }

    @Override
    public int compareTo(LinearSolution other) {
        if (other == null) {
            return 1;
        }
        if (other == this) {
            return 0;
        }
        return Arrays.compare(signature, other.signature);
    }

    @Override
    public ExprNode reifyAsExprAst() {
        assert coefficientMap.size() > 0;
        var children = new ArrayList<ExprNode>();
        for (var termC : coefficientMap.entrySet()) {
            children.add(buildAstFromTerm(termC.getValue(), termC.getKey().xPower(), termC.getKey().yPower(),
                    termC.getKey().zPower()));
        }
        if (children.size() == 1) {
            return children.get(0);
        } else {
            return new AddNode(children.toArray(ExprNode[]::new));
        }
    }

    private ExprNode buildAstFromTerm(int coeff, int xOrder, int yOrder, int zOrder) {
        assert coeff > 0 && xOrder >= 0 && yOrder >= 0 && zOrder >= 0;
        var children = new ArrayList<ExprNode>();
        if (coeff > 1) {
            children.add(new ExprConstNode(coeff));
        }
        for (int i = 0; i < xOrder; ++i) {
            children.add(VariableNode.VAR_X);
        }
        for (int i = 0; i < yOrder; ++i) {
            children.add(VariableNode.VAR_Y);
        }
        for (int i = 0; i < zOrder; ++i) {
            children.add(VariableNode.VAR_Z);
        }

        // Special case: if this is a unit term, we will have not added *any* children
        if (children.size() == 0) {
            return new ExprConstNode(1);
        } else if (children.size() == 1) {
            return children.get(0);
        } else {
            return new MultiplyNode(children.toArray(ExprNode[]::new));
        }
    }

    @Override
    public ParseNode reifyAsExprParse() {
        return reifyAsExprAst().reify();
    }

    @Override
    public int evalExpr(Environment env) {
        int sum = 0;
        for (var e : coefficientMap.entrySet()) {
            sum += e.getValue() * e.getKey().evalTerm(env);
        }
        return sum;
    }
}
