package synth.core;

import java.util.Iterator;
import java.util.Map;

import synth.cfg.Production;

public class PreorderInterpreter {

    /**
     * A static method to evaluate a program given an environment.
     *
     * @param program     the program to be evaluated
     * @param environment mapping from all variable names to their values
     * @return the value of the program expression
     */
    public static int evaluate(Iterator<Production> program, Map<String, Integer> environment) {
        PreorderInterpreter interpreter = new PreorderInterpreter(environment);
        return interpreter.evalExpr(program);
    }

    /**
     * mapping from all variable names to their values
     */
    private final Map<String, Integer> environment;

    public PreorderInterpreter(Map<String, Integer> environment) {
        this.environment = environment;
    }

    public int evalExpr(Iterator<Production> expr) {
        var sym = expr.next().getOperator();
        switch (sym) {
            case Ite:
                return evalIte(expr);
            case Add:
                return evalAdd(expr);
            case Multiply:
                return evalMultiply(expr);
            case VarX: return environment.get("x");
            case VarY: return environment.get("y");
            case VarZ: return environment.get("z");
            case Const1: return 1;
            case Const2: return 2;
            case Const3: return 3;
            default:
                throw new RuntimeException("Cannot evaluate expression " + expr);
        }
    }

    public boolean evalPred(Iterator<Production> pred) {
        var sym = pred.next().getOperator();
        switch (sym) {
            case Lt:
                return evalLt(pred);
            case Eq:
                return evalEq(pred);
            case And:
                return evalAnd(pred);
            case Or:
                return evalOr(pred);
            case Not:
                return evalNot(pred);
            default:
                throw new RuntimeException("Cannot evaluate predicate " + pred);
        }
    }

    public int evalIte(Iterator<Production> ite) {
        var predVal = evalPred(ite);
        var thenVal = evalExpr(ite);
        var elseVal = evalExpr(ite);
        if (predVal) {
            return thenVal;
        } else {
            return elseVal;
        }
    }

    public int evalAdd(Iterator<Production> add) {
        var valA = evalExpr(add);
        var valB = evalExpr(add);
        return valA + valB;
    }

    public int evalMultiply(Iterator<Production> multiply) {
        var valA = evalExpr(multiply);
        var valB = evalExpr(multiply);
        return valA * valB;
    }

    public boolean evalLt(Iterator<Production> lt) {
        var valA = evalExpr(lt);
        var valB = evalExpr(lt);
        return valA < valB;
    }

    public boolean evalEq(Iterator<Production> eq) {
        var valA = evalExpr(eq);
        var valB = evalExpr(eq);
        return valA == valB;
    }

    public boolean evalAnd(Iterator<Production> and) {
        var valA = evalPred(and);
        var valB = evalPred(and);
        return valA && valB;
    }

    public boolean evalOr(Iterator<Production> or) {
        var valA = evalPred(or);
        var valB = evalPred(or);
        return valA && valB;
    }

    public boolean evalNot(Iterator<Production> not) {
        var val = evalPred(not);
        return !val;
    }
}
