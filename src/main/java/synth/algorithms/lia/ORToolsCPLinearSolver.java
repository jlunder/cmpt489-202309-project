package synth.algorithms.lia;

import java.util.*;
import java.util.logging.*;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;

public class ORToolsCPLinearSolver extends LinearSolver {
    private static Logger logger = Logger.getLogger(ORToolsCPLinearSolver.class.getName());

    static {
        Loader.loadNativeLibraries();
    }

    private static class SolveLimits {
        private List<Term> terms = null;
        private int maxConst = 0;

        public List<Term> terms() {
            return terms;
        }

        public int maxConst() {
            return maxConst;
        }

        SolveLimits(int maxOrder, int maxConst) {
            this.terms = Term.makeUpToOrder(maxOrder);
            this.maxConst = maxConst;
        }
    }

    List<SolveLimits> solveLimits = List.of(new SolveLimits(2, 16), new SolveLimits(3, 32), new SolveLimits(4, 64));
    int timeoutMs = 5000;

    public ORToolsCPLinearSolver(Xoshiro256SS rng) {
        super(rng);
    }

    @Override
    protected LinearSolution solveSubset(Collection<Example> exampleSubset) {
        for (var lim : solveLimits) {
            var sol = trySolveSubset(lim, exampleSubset);
            if (sol != null && !sol.coefficients().isEmpty()) {
                // A real solution!
                return sol;
            }
            // Relax limits per our schedule and try again?
        }
        // Didn't find anything in the end -- probably means an intractable
        // contradiction (we could maybe determing this more conclusively/without just
        // brute forcing if we examine the OR-Tools Solver's feedback below)
        return null;
    }

    protected LinearSolution trySolveSubset(SolveLimits limits, Collection<Example> exampleSubset) {
        logger.log(Level.INFO, "Trying solve with {0} terms, max const {1}",
                new Object[] { limits.terms().size(), limits.maxConst() });
        CpModel model = new CpModel();

        var termVars = new HashMap<Term, IntVar>();
        for (var t : limits.terms()) {
            termVars.put(t, model.newIntVar(0, limits.maxConst(), t.name()));
        }

        int i = 0;
        for (var ex : exampleSubset) {
            var usedTerms = new ArrayList<IntVar>();
            var coeffs = new ArrayList<Long>();
            for (var t : limits.terms()) {
                int coeff = t.evalTerm(ex.input());
                if (coeff != 0) {
                    usedTerms.add(termVars.get(t));
                    coeffs.add((long) coeff);
                }
            }
            var coeffsArray = new long[coeffs.size()];
            for (i = 0; i < coeffsArray.length; ++i) {
                coeffsArray[i] = coeffs.get(i);
            }
            model.addEquality(LinearExpr.weightedSum(usedTerms.toArray(LinearArgument[]::new), coeffsArray),
                    model.newConstant(ex.output()));
        }

        var sols = new ArrayList<LinearSolution>();
        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(true);
        var solutionCb = new CpSolverSolutionCallback() {
            private static final int maxSolutions = 1;
            private Exception callbackException = null;

            @Override
            public void onSolutionCallback() {
                try {
                    callbackException = null;
                    var termCoeffs = new HashMap<Term, Integer>();
                    for (var e : termVars.entrySet()) {
                        var coeff = (int) value(e.getValue());
                        if (coeff != 0) {
                            termCoeffs.put(e.getKey(), coeff);
                            logger.log(Level.INFO, e.getKey().name() + " = " + coeff);
                        }
                    }
                    sols.add(new LinearSolution(termCoeffs));
                    if (sols.size() >= maxSolutions) {
                        stopSearch();
                    }
                } catch (Exception e) {
                    callbackException = e;
                    stopSearch();
                }
            }
        };
        solver.getParameters().setMaxTimeInSeconds(timeoutMs / 1000d);
        final CpSolverStatus resultStatus = solver.solve(model, solutionCb);

        if (solutionCb.callbackException != null) {
            logger.log(Level.SEVERE, "Error during solution enumeration", solutionCb.callbackException);
            return null;
        } else if (sols.isEmpty()) {
            logger.log(Level.WARNING, "No satisfying solution found! Solver returned {0}",
                    new Object[] { resultStatus });
            return null;
        }

        return sols.get(0);
    }
}
