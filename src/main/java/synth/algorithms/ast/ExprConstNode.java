package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class ExprConstNode extends ExprNode {
    public static ParseNode REIFIED_1 = new ParseNode(Symbol.Const1, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_2 = new ParseNode(Symbol.Const2, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_3 = new ParseNode(Symbol.Const3, ParseNode.NO_CHILDREN);
    public static ParseNode REIFIED_4 = new ParseNode(Symbol.Add, List.of(REIFIED_2, REIFIED_2));
    public static ParseNode REIFIED_5 = new ParseNode(Symbol.Add, List.of(REIFIED_2, REIFIED_3));
    public static ParseNode REIFIED_6 = new ParseNode(Symbol.Add, List.of(REIFIED_3, REIFIED_3));

    private static final int REIFY_BASE = 3;
    private static final int REIFY_SMALL_VALUE_MAX = 6;
    private static final HashMap<Integer, ParseNode> reifiedCache = new HashMap<>();

    private final int value;

    public ExprConstNode(int value) {
        assert value > 0;
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public List<AstNode> children() {
        return NO_CHILDREN;
    }

    public int evalExpr(Environment env) {
        return value;
    }

    public ParseNode reify() {
        return computeReified(this.value);
    }

    private static ParseNode computeReified(int value) {
        return reifiedCache.computeIfAbsent(value, val -> {
            // Should already be seeded with 1-3, < 1 is impossible
            if (val <= REIFY_SMALL_VALUE_MAX)
                return reifiedSmallValue(value);
            int divVal = val / REIFY_BASE;
            int modVal = val % REIFY_BASE;
            assert divVal > 0;
            if (modVal == 0) {
                return new ParseNode(Symbol.Multiply, List.of(
                        reifiedSmallValue(REIFY_BASE), computeReified(divVal)));
            }
            return new ParseNode(Symbol.Add, List.of(reifiedSmallValue(modVal),
                    new ParseNode(Symbol.Multiply, List.of(reifiedSmallValue(REIFY_BASE), computeReified(divVal)))));
        });
    }

    private static ParseNode reifiedSmallValue(int value) {
        switch (value) {
            case 1:
                return REIFIED_1;
            case 2:
                return REIFIED_2;
            case 3:
                return REIFIED_3;
            case 4:
                return REIFIED_4;
            case 5:
                return REIFIED_5;
            case 6:
                return REIFIED_6;
            default:
                throw new IllegalArgumentException("requires 1 <= value <= " + REIFY_SMALL_VALUE_MAX);
        }
    }
}
