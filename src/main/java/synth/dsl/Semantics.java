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
                throw new RuntimeException("Cannot evaluate expression " + expr);
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
                throw new RuntimeException("Cannot evaluate predicate " + pred);
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
                throw new RuntimeException("Cannot evaluate expression " + sym);
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
                throw new RuntimeException("Cannot evaluate predicate " + pred);
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
}
