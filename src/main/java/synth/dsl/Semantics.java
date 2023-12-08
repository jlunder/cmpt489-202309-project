package synth.dsl;

import java.util.*;

import synth.core.*;

public class Semantics {

    /**
     * A static method to evaluate a program given an environment.
     *
     * @param program     the program to be evaluated
     * @param environment mapping from all variable names to their values
     * @return the value of the program expression
     */
    public static int evaluate(Program program, Environment env) {
        return evalExpr(program.getRoot(), env);
    }

    public static int evaluate(ParseNode program, Environment env) {
        return evalExpr(program, env);
    }

    private static int evalExpr(ParseNode expr, Environment env) {
        switch (expr.getSymbol()) {
            case Ite:
                return evalIte(expr, env);
            case Add:
                return evalAdd(expr, env);
            case Multiply:
                return evalMultiply(expr, env);
            case VarX:
                return env.x();
            case VarY:
                return env.y();
            case VarZ:
                return env.z();
            case Const1:
                return 1;
            case Const2:
                return 2;
            case Const3:
                return 3;
            default:
                throw new UnsupportedOperationException("Cannot evaluate expression " + expr);
        }
    }

    private static boolean evalPred(ParseNode pred, Environment env) {
        switch (pred.getSymbol()) {
            case Lt:
                return evalLt(pred, env);
            case Eq:
                return evalEq(pred, env);
            case And:
                return evalAnd(pred, env);
            case Or:
                return evalOr(pred, env);
            case Not:
                return evalNot(pred, env);
            default:
                throw new UnsupportedOperationException("Cannot evaluate predicate " + pred);
        }
    }

    private static int evalIte(ParseNode ite, Environment env) {
        if (evalPred(ite.getChild(0), env)) {
            return evalExpr(ite.getChild(1), env);
        } else {
            return evalExpr(ite.getChild(2), env);
        }
    }

    private static int evalAdd(ParseNode add, Environment env) {
        return evalExpr(add.getChild(0), env) + evalExpr(add.getChild(1), env);
    }

    private static int evalMultiply(ParseNode multiply, Environment env) {
        return evalExpr(multiply.getChild(0), env) * evalExpr(multiply.getChild(1), env);
    }

    private static boolean evalLt(ParseNode lt, Environment env) {
        return evalExpr(lt.getChild(0), env) < evalExpr(lt.getChild(1), env);
    }

    private static boolean evalEq(ParseNode eq, Environment env) {
        return evalExpr(eq.getChild(0), env) == evalExpr(eq.getChild(1), env);
    }

    private static boolean evalAnd(ParseNode and, Environment env) {
        return evalPred(and.getChild(0), env) && evalPred(and.getChild(1), env);
    }

    private static boolean evalOr(ParseNode or, Environment env) {
        return evalPred(or.getChild(0), env) || evalPred(or.getChild(1), env);
    }

    private static boolean evalNot(ParseNode not, Environment env) {
        return !evalPred(not.getChild(0), env);
    }

    /**
     * A static method to evaluate a program given an env.
     *
     * @param program the program to be evaluated
     * @param env     mapping from all variable names to their values
     * @return the value of the program expression
     */
    public static int evaluate(Iterator<Symbol> program, Environment env) {
        return evalExpr(program, env);
    }

    private static int evalExpr(Iterator<Symbol> expr, Environment env) {
        var sym = expr.next();
        switch (sym) {
            case Ite:
                return evalIte(expr, env);
            case Add:
                return evalAdd(expr, env);
            case Multiply:
                return evalMultiply(expr, env);
            case VarX:
                return env.x();
            case VarY:
                return env.y();
            case VarZ:
                return env.z();
            case Const1:
                return 1;
            case Const2:
                return 2;
            case Const3:
                return 3;
            default:
                throw new UnsupportedOperationException("Cannot evaluate expression " + sym);
        }
    }

    private static boolean evalPred(Iterator<Symbol> pred, Environment env) {
        var sym = pred.next();
        switch (sym) {
            case Lt:
                return evalLt(pred, env);
            case Eq:
                return evalEq(pred, env);
            case And:
                return evalAnd(pred, env);
            case Or:
                return evalOr(pred, env);
            case Not:
                return evalNot(pred, env);
            default:
                throw new UnsupportedOperationException("Cannot evaluate predicate " + pred);
        }
    }

    private static int evalIte(Iterator<Symbol> ite, Environment env) {
        var predVal = evalPred(ite, env);
        var thenVal = evalExpr(ite, env);
        var elseVal = evalExpr(ite, env);
        if (predVal) {
            return thenVal;
        } else {
            return elseVal;
        }
    }

    private static int evalAdd(Iterator<Symbol> add, Environment env) {
        var valA = evalExpr(add, env);
        var valB = evalExpr(add, env);
        return valA + valB;
    }

    private static int evalMultiply(Iterator<Symbol> multiply, Environment env) {
        var valA = evalExpr(multiply, env);
        var valB = evalExpr(multiply, env);
        return valA * valB;
    }

    private static boolean evalLt(Iterator<Symbol> lt, Environment env) {
        var valA = evalExpr(lt, env);
        var valB = evalExpr(lt, env);
        return valA < valB;
    }

    private static boolean evalEq(Iterator<Symbol> eq, Environment env) {
        var valA = evalExpr(eq, env);
        var valB = evalExpr(eq, env);
        return valA == valB;
    }

    private static boolean evalAnd(Iterator<Symbol> and, Environment env) {
        var valA = evalPred(and, env);
        var valB = evalPred(and, env);
        return valA && valB;
    }

    private static boolean evalOr(Iterator<Symbol> or, Environment env) {
        var valA = evalPred(or, env);
        var valB = evalPred(or, env);
        return valA || valB;
    }

    private static boolean evalNot(Iterator<Symbol> not, Environment env) {
        var val = evalPred(not, env);
        return !val;
    }

    private static class PostOrderEvaluator {
        private static final int MAX_STACK = 1000;

        private int[] exprStack = new int[MAX_STACK];
        private ParseNode[] exprNodeStack = new ParseNode[MAX_STACK];
        private int[] exprSizeStack = new int[MAX_STACK];
        private int exprTop;
        private boolean[] boolStack = new boolean[MAX_STACK];
        private ParseNode[] boolNodeStack = new ParseNode[MAX_STACK];
        private int[] boolSizeStack = new int[MAX_STACK];
        private int boolTop;

        public int evaluateExpr(Symbol[] program, Environment env) {
            evaluateProgram(program, env);
            return popExpr();
        }

        public boolean evaluateBool(Symbol[] program, Environment env) {
            evaluateProgram(program, env);
            return popBool();
        }

        public void evaluateProgram(Symbol[] program, Environment env) {
            reset(program);
            for (int i = 0; i < program.length; ++i) {
                var s = program[i];
                if (s == null) {
                    continue;
                }
                int ex, ey;
                boolean bx, by;
                switch (s) {
                    case Const1:
                        pushExpr(1);
                        break;
                    case Const2:
                        pushExpr(2);
                        break;
                    case Const3:
                        pushExpr(3);
                        break;
                    case VarX:
                        pushExpr(env.x());
                        break;
                    case VarY:
                        pushExpr(env.y());
                        break;
                    case VarZ:
                        pushExpr(env.z());
                        break;
                    case Ite:
                        bx = popBool();
                        ex = popExpr();
                        ey = popExpr();
                        pushExpr(bx ? ex : ey);
                        break;
                    case Add:
                        ex = popExpr();
                        ey = popExpr();
                        pushExpr(ex + ey);
                        break;
                    case Multiply:
                        ex = popExpr();
                        ey = popExpr();
                        pushExpr(ex * ey);
                        break;
                    case Lt:
                        ex = popExpr();
                        ey = popExpr();
                        pushBool(ex < ey);
                        break;
                    case Eq:
                        ex = popExpr();
                        ey = popExpr();
                        pushBool(ex == ey);
                        break;
                    case And:
                        bx = popBool();
                        by = popBool();
                        pushBool(bx && by);
                        break;
                    case Or:
                        bx = popBool();
                        by = popBool();
                        pushBool(bx || by);
                        break;
                    case Not:
                        bx = popBool();
                        pushBool(!bx);
                        break;
                    default:
                        throw new UnsupportedOperationException("Cannot evaluate expression " + s);
                }
            }
        }

        public ParseNode makeExprParseTree(Symbol[] program) {
            makeParseTree(program);
            return popExprNode();
        }

        public ParseNode makeBoolParseTree(Symbol[] program) {
            makeParseTree(program);
            return popBoolNode();
        }

        private void makeParseTree(Symbol[] program) {
            reset(program);
            for (int i = 0; i < program.length; ++i) {
                var s = program[i];
                if (s == null) {
                    continue;
                }
                ParseNode c, x, y;
                switch (s) {
                    case Const1:
                        pushExprNode(ParseNode.CONST_1);
                        break;
                    case Const2:
                        pushExprNode(ParseNode.CONST_2);
                        break;
                    case Const3:
                        pushExprNode(ParseNode.CONST_3);
                        break;
                    case VarX:
                        pushExprNode(ParseNode.VAR_X);
                        break;
                    case VarY:
                        pushExprNode(ParseNode.VAR_Y);
                        break;
                    case VarZ:
                        pushExprNode(ParseNode.VAR_Z);
                        break;
                    case Ite:
                        c = popBoolNode();
                        x = popExprNode();
                        y = popExprNode();
                        pushExprNode(new ParseNode(Symbol.Ite, List.of(c, x, y)));
                        break;
                    case Add:
                    case Multiply:
                        x = popExprNode();
                        y = popExprNode();
                        pushExprNode(new ParseNode(s, List.of(x, y)));
                        break;
                    case Lt:
                    case Eq:
                        x = popExprNode();
                        y = popExprNode();
                        pushBoolNode(new ParseNode(s, List.of(x, y)));
                        break;
                    case And:
                    case Or:
                        x = popBoolNode();
                        y = popBoolNode();
                        pushBoolNode(new ParseNode(s, List.of(x, y)));
                        break;
                    case Not:
                        x = popBoolNode();
                        pushBoolNode(new ParseNode(Symbol.Not, List.of(x)));
                        break;
                    default:
                        throw new UnsupportedOperationException("Cannot evaluate expression " + s);
                }
            }
        }

        public int measureExprParseTree(Symbol[] program) {
            measureParseTree(program);
            return popExprSize();
        }

        public int measureBoolParseTree(Symbol[] program) {
            measureParseTree(program);
            return popBoolSize();
        }

        private void measureParseTree(Symbol[] program) {
            reset(program);
            for (int i = 0; i < program.length; ++i) {
                var s = program[i];
                if (s == null) {
                    continue;
                }
                int c, x, y;
                switch (s) {
                    case Const1:
                    case Const2:
                    case Const3:
                    case VarX:
                    case VarY:
                    case VarZ:
                        pushExprSize(1);
                        break;
                    case Ite:
                        c = popBoolSize();
                        x = popExprSize();
                        y = popExprSize();
                        pushExprSize(1 + c + x + y);
                        break;
                    case Add:
                    case Multiply:
                        x = popExprSize();
                        y = popExprSize();
                        pushExprSize(1 + x + y);
                        break;
                    case Lt:
                    case Eq:
                        x = popExprSize();
                        y = popExprSize();
                        pushBoolSize(1 + x + y);
                        break;
                    case And:
                    case Or:
                        x = popBoolSize();
                        y = popBoolSize();
                        pushBoolSize(1 + x + y);
                        break;
                    case Not:
                        x = popBoolSize();
                        pushBoolSize(1 + x);
                        break;
                    default:
                        throw new UnsupportedOperationException("Cannot evaluate expression " + s);
                }
            }
        }

        private void reset(Symbol[] program) {
            if (program.length > MAX_STACK) {
                throw new UnsupportedOperationException("Program too long");
            }
            this.exprTop = 0;
            this.boolTop = 0;
        }

        private void pushExpr(int value) {
            exprStack[exprTop++] = value;
        }

        private int popExpr() {
            if (exprTop > 0) {
                return exprStack[--exprTop];
            } else {
                return 1;
            }
        }

        private void pushBool(boolean value) {
            boolStack[boolTop++] = value;
        }

        private boolean popBool() {
            if (boolTop > 0) {
                return boolStack[--boolTop];
            } else {
                return false;
            }
        }

        private void pushExprNode(ParseNode node) {
            exprNodeStack[exprTop++] = node;
        }

        private ParseNode popExprNode() {
            if (exprTop > 0) {
                return exprNodeStack[--exprTop];
            } else {
                return ParseNode.CONST_1;
            }
        }

        private void pushBoolNode(ParseNode node) {
            boolNodeStack[boolTop++] = node;
        }

        private ParseNode popBoolNode() {
            if (boolTop > 0) {
                return boolNodeStack[--boolTop];
            } else {
                return ParseNode.CONST_FALSE;
            }
        }

        private void pushExprSize(int size) {
            exprSizeStack[exprTop++] = size;
        }

        private int popExprSize() {
            if (exprTop > 0) {
                return exprSizeStack[--exprTop];
            } else {
                return 1;
            }
        }

        private void pushBoolSize(int size) {
            boolSizeStack[boolTop++] = size;
        }

        private int popBoolSize() {
            if (boolTop > 0) {
                return boolSizeStack[--boolTop];
            } else {
                // CONST_FALSE is Eq(1, 2) -- actually 3 nodes
                return 3;
            }
        }
    }

    private static ThreadLocal<PostOrderEvaluator> postOrderEvaluator = new ThreadLocal<>();

    public static int evaluateExprPostOrder(Symbol[] program, Environment env) {
        ensureThreadLocals();
        return postOrderEvaluator.get().evaluateExpr(program, env);
    }

    public static boolean evaluateBoolPostOrder(Symbol[] program, Environment env) {
        ensureThreadLocals();
        return postOrderEvaluator.get().evaluateBool(program, env);
    }

    public static ParseNode makeExprParseTreeFromPostOrder(Symbol[] program) {
        ensureThreadLocals();
        return postOrderEvaluator.get().makeExprParseTree(program);
    }

    public static ParseNode makeBoolParseTreeFromPostOrder(Symbol[] program) {
        ensureThreadLocals();
        return postOrderEvaluator.get().makeBoolParseTree(program);
    }

    public static int measureExprParseTreeFromPostOrder(Symbol[] program) {
        ensureThreadLocals();
        return postOrderEvaluator.get().measureExprParseTree(program);
    }

    public static int measureBoolParseTreeFromPostOrder(Symbol[] program) {
        ensureThreadLocals();
        return postOrderEvaluator.get().measureBoolParseTree(program);
    }

    private static void ensureThreadLocals() {
        if (postOrderEvaluator.get() == null) {
            postOrderEvaluator.set(new PostOrderEvaluator());
        }
    }

}
