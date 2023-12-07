package synth.algorithms.lia;

import java.util.*;
import java.util.logging.*;

import com.microsoft.z3.*;

import synth.algorithms.classify.Classification;
import synth.algorithms.classify.PartialSolution;
import synth.algorithms.rng.Xoshiro256SS;
import synth.core.Example;

public class Z3LinearSolver extends LinearSolver {
    private static Logger logger = Logger.getLogger(Z3LinearSolver.class.getName());

    protected List<Term> terms = null;
    protected int maxConst = 0;
    protected int maxSols = 5;
    private Context z3;

    public Z3LinearSolver(Xoshiro256SS rng, int maxOrder, int maxConst) {
        super(rng);
        this.terms = Term.makeUpToOrder(maxOrder);
        this.maxConst = maxConst;
    }

    public static final int SHORT_TIMEOUT_MS = 5000;
    // public static final int SHORT_TIMEOUT_MS = 500;

    /**
     * For each example, compute a solution set using the linear solver.
     */
    @Override
    public Collection<PartialSolution> computeSolutionSets(List<Example> examples) throws InterruptedException {
        try (var z3Managed = new Context()) {
            z3 = z3Managed;

            assert examples.size() > 0;

            // avoid accidental or malicious bias
            var scrambledExamples = randomOrder(examples);
            var nonNegativeInputExamples = List.of(scrambledExamples.stream()
                    .filter(e -> e.input().x() >= 0 && e.input().y() >= 0 && e.input().z() >= 0)
                    .toArray(Example[]::new));

            var solutions = new HashSet<LinearSolution>();
            var ungroupedExamples = new HashSet<Example>(examples);

            for (var exampleSubset : List.of(nonNegativeInputExamples, scrambledExamples)) {
                for (var ei : exampleSubset) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException("Interrupted in LinearSolver::computeSolutionSets()");
                    }

                    if (!ungroupedExamples.contains(ei)) {
                        // Already dealt with this one in an earlier pass
                        continue;
                    }

                    // Start a new group for this example
                    var included = new HashSet<Example>();
                    included.add(ei);

                    logger.log(Level.INFO, "Building group around: {0}", new Object[] { ei });

                    // For Z3, we only use the subset of examples, because its performance might be
                    // affected by negative numbers. Not so the completion step, so we always let it
                    // have the full example set.
                    var res = findMoreGroupMembersWithZ3(exampleSubset, included);
                    res = completeGroupUsingSolutions(res.solutions(), examples, included);
                    solutions.addAll(res.solutions());
                    ungroupedExamples.removeAll(included);
                }
            }

            return Set.of(solutions.stream()
                    .map(sol -> new PartialSolution(sol, Classification.makeFromExamples(sol, examples)))
                    .toArray(PartialSolution[]::new));
        }
    }

    private SolveResult findMoreGroupMembersWithZ3(Collection<Example> examples, HashSet<Example> included)
            throws InterruptedException {
        var ungroupedExamples = new HashSet<Example>(examples);
        var scrambledExamples = randomOrder(examples);
        assert included.size() == 1;
        // Test against all other examples, adding more until there's enough in the
        // group for it to have a definite solution
        SolveResult res = null;
        int rejectCount = 0;

        ungroupedExamples.removeAll(included);

        for (var ej : List.copyOf(scrambledExamples)) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Interrupted in LinearSolver::findMoreGroupMembersWithZ3()");
            }

            if (!ungroupedExamples.contains(ej)) {
                // Already dealt with this one in an earlier pass
                continue;
            }

            // Set up the Z3 solve session
            var sess = startSession(SHORT_TIMEOUT_MS);
            for (var ei : included) {
                sess.addEquation(ei);
            }
            sess.addEquation(ej);

            if (sess.checkSatisfiable()) {
                logger.log(Level.INFO, "-- Accepted by Z3: {0}", new Object[] { ej });
                included.add(ej);
                ungroupedExamples.remove(ej);
                res = sess.solve();
                if (included.size() > 3) {
                    res = completeGroupUsingSolutions(res.solutions(), ungroupedExamples, included);
                    ungroupedExamples.removeAll(included);
                }
            } else {
                logger.log(Level.INFO, "-- Rejected by Z3: {0}", new Object[] { ej });
                ++rejectCount;
                if (rejectCount > 3 && res != null) {
                    return res;
                }
            }
        }

        // If there are at least 2 things in the set there must be a result from the
        // solve() call left over...
        if (res != null) {
            return res;
        }
        assert included.size() == 1;
        // Degenerate case -- not enough examples to be able to come up with a good
        // solution, put the loner in its own group
        var ex = included.iterator().next();
        if (ex.output() > 0) {
            // Generate a constant as our solution
            return new SolveResult(false, List.of(new LinearSolution(Map.of(Term.TERM_1, ex.output()))));
        } else {
            // Ugh, it's not a positive number so we can't just generate a constant. Maybe
            // Z3 has an idea?
            var sess = startSession(SHORT_TIMEOUT_MS);
            sess.addEquation(ex);
            return sess.solve();
        }
    }

    public SolveSession startSession(int timeoutMs) {
        return new SolveSession(timeoutMs);
    }

    public class SolveSession {
        private Solver z3Solver = z3.mkSolver();
        private IntExpr z3Zero = z3.mkInt(0);
        private IntExpr z3CMax = z3.mkInt(maxConst);
        private HashMap<Term, IntExpr> z3Coeffs = new HashMap<>();
        private Status z3Status = Status.UNKNOWN;

        SolveSession(int timeoutMs) {
            var params = z3.mkParams();
            params.add("timeout", timeoutMs);
            z3Solver.setParameters(params);
        }

        @SuppressWarnings("unchecked")
        public void addEquation(Example example) {
            if (z3Status == Status.UNSATISFIABLE) {
                // not much point continuing
                return;
            }
            z3Status = Status.UNKNOWN;

            var z3Consts = new ArrayList<ArithExpr<IntSort>>();
            for (var term : terms) {
                var z3C = z3Coeffs.computeIfAbsent(term, t -> {
                    var newC = z3.mkIntConst(t.name());
                    // Term constants must be >= 0
                    z3Solver.add(z3.mkGe(newC, z3Zero));
                    // Term constants must be <= CMax -- unreasonably large constants imply a deep
                    // parse tree, and we should limit the complexity of the constants we generate
                    // in proportion with other complexity limits.
                    // (If term constants aren't bounded, and the example has negative numbers in
                    // the input, the solver can spend a lot of time thinking about how different
                    // combinations of even and odd powers of the inputs could add together to make
                    // the precise target values you're looking for.)
                    z3Solver.add(z3.mkLe(newC, z3CMax));
                    return newC;
                });
                z3Consts.add(z3.mkMul(z3.mkInt(term.evalTerm(example.input())), z3C));
            }
            var sum = z3.mkAdd(z3Consts.toArray(ArithExpr[]::new));
            var eqn = z3.mkEq(sum, z3.mkInt(example.output()));
            z3Solver.add(eqn);
        }

        public boolean checkSatisfiable() throws InterruptedException {
            if (z3Status == Status.UNKNOWN) {
                z3Status = z3Solver.check();
            }
            if (Thread.interrupted()) {
                throw new InterruptedException("Interrupted in LinearSolver::checkSatisfiable()");
            }
            if (z3Status == Status.SATISFIABLE) {
                return true;
            } else {
                return false;
            }
        }

        public SolveResult solve() throws InterruptedException {
            var solutions = new ArrayList<LinearSolution>();

            while (checkSatisfiable() && solutions.size() < maxSols) {
                var z3Model = z3Solver.getModel();
                var solMap = new HashMap<Term, Integer>(z3Coeffs.size());
                for (var entry : z3Coeffs.entrySet()) {
                    int coeff = ((IntNum) (z3Model.getConstInterp(entry.getValue()))).getInt();
                    assert coeff >= 0;
                    if (coeff > 0) {
                        solMap.put(entry.getKey(), coeff);
                    }
                }
                solutions.add(new LinearSolution(solMap));
                // That was a nice solution, let's try something different
                addBlockingClause(z3Solver, z3Coeffs, solMap);
            }

            return new SolveResult(solutions.size() < maxSols, solutions);
        }

        @SuppressWarnings("unchecked")
        private void addBlockingClause(Solver solver, HashMap<Term, IntExpr> z3Coeffs,
                HashMap<Term, Integer> disallowedSolution) {
            assert z3Status == Status.SATISFIABLE;
            z3Status = Status.UNKNOWN;
            var clause = z3.mkOr(disallowedSolution.entrySet().stream()
                    .map(e -> z3.mkNot(z3.mkEq(z3Coeffs.get(e.getKey()), z3.mkInt(e.getValue()))))
                    .toArray(Expr[]::new));
            z3Solver.add(clause);
        }
    }
}
