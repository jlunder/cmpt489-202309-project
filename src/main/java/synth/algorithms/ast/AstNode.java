package synth.algorithms.ast;

import java.util.*;

import synth.core.Environment;
import synth.core.ParseNode;
import synth.dsl.*;
import synth.util.ArraySliceList;

public abstract class AstNode {
    public static final AstNode[] NO_CHILDREN = new AstNode[0];

    protected AstNode[] childrenArray;
    protected List<AstNode> childrenList;
    protected ParseNode reified = null;

    public AstNode(AstNode... children) {
        childrenArray = children;
    }

    public int numChildren() {
        return childrenArray.length;
    }

    public AstNode child(int i) {
        return childrenArray[i];
    }

    public List<AstNode> children() {
        if (childrenList == null) {
            childrenList = new ArraySliceList<>(childrenArray);
        }
        return childrenList;
    }

    public abstract boolean evalBool(Environment env);

    public abstract int evalExpr(Environment env);

    public ParseNode reify() {
        if (reified == null) {
            reified = makeReified();
        }
        return reified;
    }

    protected ParseNode makeReified() {
        throw new UnsupportedOperationException("reified should be set by constructor");
    }

    protected ParseNode makeReifiedAssociativeBinaryOperator(Symbol opSym) {
        return makeReifiedAssociativeBinaryOperator(opSym, 0, numChildren());
    }

    protected ParseNode makeReifiedAssociativeBinaryOperator(Symbol opSym, int fromIndex, int toIndex) {
        assert (fromIndex < numChildren()) && (toIndex <= numChildren()) && (fromIndex < toIndex);
        int size = toIndex - fromIndex;
        if (size <= 0) {
            return null;
        } else if (size < 2) {
            return childrenArray[fromIndex].reify();
        } else if (size == 2) {
            return new ParseNode(opSym, List.of(child(fromIndex).reify(), child(fromIndex + 1).reify()));
        } else {
            // Round towards making the list left heavy for no particular reason
            int split = fromIndex + (size + 1) / 2;
            return new ParseNode(opSym, List.of(makeReifiedAssociativeBinaryOperator(opSym, fromIndex, split),
                    makeReifiedAssociativeBinaryOperator(opSym, split, toIndex)));
        }
    }

    public abstract AstNode withChildren(AstNode... children);

    public AstNode substituteMarkers(Map<Integer, AstNode> substitution) {
        if (numChildren() == 0) {
            return this;
        } else {
            var subChildren = new AstNode[numChildren()];
            for (int i = 0; i < numChildren(); ++i) {
                subChildren[i] = child(i).substituteMarkers(substitution);
            }
            return withChildren(subChildren);
        }
    }
}
