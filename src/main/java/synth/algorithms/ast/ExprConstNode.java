package synth.algorithms.ast;

import java.util.*;

import synth.core.*;
import synth.dsl.Symbol;

public class ExprConstNode extends ExprNode {
    public static final ExprConstNode CONST_1 = new ExprConstNode(1);
    public static final ExprConstNode CONST_2 = new ExprConstNode(2);
    public static final ExprConstNode CONST_3 = new ExprConstNode(3);

    public static final ParseNode REIFIED_1 = ParseNode.CONST_1;
    public static final ParseNode REIFIED_2 = ParseNode.CONST_2;
    public static final ParseNode REIFIED_3 = ParseNode.CONST_3;
    public static final ParseNode REIFIED_4 = new ParseNode(Symbol.Add, List.of(REIFIED_2, REIFIED_2));
    public static final ParseNode REIFIED_5 = new ParseNode(Symbol.Add, List.of(REIFIED_2, REIFIED_3));
    public static final ParseNode REIFIED_6 = new ParseNode(Symbol.Add, List.of(REIFIED_3, REIFIED_3));

    private static final int REIFY_BASE = 3;
    private static final int REIFY_SMALL_VALUE_MAX = 6;
    private static final HashMap<Integer, ParseNode> reifiedCache = new HashMap<>();

    private final int value;

    public ExprConstNode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public int evalExpr(Environment env) {
        return value;
    }

    @Override
    protected ParseNode makeReified() {
        return computeReified(value);
    }

    private static ParseNode computeReified(int value) {
        assert value > 0;
        var reified = reifiedCache.get(value);
        if(reified == null) {
            // Should already be seeded with 1-3, < 1 is impossible
            if (value <= REIFY_SMALL_VALUE_MAX)
                return reifySmallValue(value);
            int divVal = value / REIFY_BASE;
            int modVal = value % REIFY_BASE;
            assert divVal > 0;
            if (modVal == 0) {
                return new ParseNode(Symbol.Multiply, List.of(
                        reifySmallValue(REIFY_BASE), computeReified(divVal)));
            }
            reified = new ParseNode(Symbol.Add, List.of(reifySmallValue(modVal),
                    new ParseNode(Symbol.Multiply, List.of(reifySmallValue(REIFY_BASE), computeReified(divVal)))));
            reifiedCache.put(value, reified);
        }
        return reified;
    }

    private static ParseNode reifySmallValue(int value) {
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

    public AstNode withChildren(AstNode... children) {
        return new ExprConstNode(value);
    }
}
