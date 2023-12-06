package synth.algorithms.lia;

import java.util.*;
import java.util.logging.*;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.*;

import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;

public class ORToolsMPLinearSolver extends LinearSolver {
    private static Logger logger = Logger.getLogger(ORToolsMPLinearSolver.class.getName());

    static {
        Loader.loadNativeLibraries();
    }

    protected List<Term> terms = null;
    protected int maxConst = 0;
    protected int maxSols = 5;

    public ORToolsMPLinearSolver(Xoshiro256SS rng, int maxOrder, int maxConst) {
        super(rng);
        var newTerms = new ArrayList<Term>();
        for (int i = 0; ; ++i) {
            var t = Term.fromIndex(i);
            if (t.xPower() > maxOrder && t.yPower() > maxOrder && t.zPower() > maxOrder) {
                break;
            }
            if (t.xPower() <= maxOrder && t.yPower() <= maxOrder && t.zPower() <= maxOrder) {
                newTerms.add(t);
            }
        }
        this.terms = newTerms;
        this.maxConst = maxConst;
    }

    @Override
    protected LinearSolution solveSubset(Collection<Example> exampleSubset) {
        MPSolver solver = MPSolver.createSolver("SCIP");
        if (solver == null) {
            throw new IllegalStateException("Could not create solver SCIP");
        }

        var termVars = new HashMap<Term, MPVariable>();
        for (var t : terms) {
            termVars.put(t, solver.makeIntVar(0, maxConst, t.name()));
        }

        int i = 0;
        for (var ex : exampleSubset) {
            MPConstraint eqn = solver.makeConstraint(ex.output() - 0.5, ex.output() + 0.5, String.format("ex%d", i++));
            for (var t : terms) {
                int coeff = t.evalTerm(ex.input());
                if (coeff != 0) {
                    eqn.setCoefficient(termVars.get(t), coeff);
                }
            }
        }

        var objective = solver.objective();
        objective.setMinimization();
        for (var v : termVars.values()) {
            objective.setCoefficient(v, 1);
        }

        final MPSolver.ResultStatus resultStatus = solver.solve();
        if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
            logger.log(Level.INFO, "Solution:");
            logger.log(Level.INFO, "Objective value = " + objective.value());

            var termCoeffs = new HashMap<Term, Integer>();
            for (var e : termVars.entrySet()) {
                var coeff = (int) e.getValue().solutionValue();
                if (coeff != 0) {
                    termCoeffs.put(e.getKey(), coeff);
                    logger.log(Level.INFO, e.getKey().name() + " = " + coeff);
                }
            }
            return new LinearSolution(termCoeffs);
        } else {
            logger.log(Level.WARNING, "The problem does not have an optimal solution!");
            return null;
        }
    }
}
