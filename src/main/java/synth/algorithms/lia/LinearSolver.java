package synth.algorithms.lia;

import java.util.*;

import com.microsoft.z3.*;

import synth.algorithms.classify.Classification;
import synth.core.Example;

public class LinearSolver implements AutoCloseable {
    private static final boolean logGroups = true;

    private List<Term> terms;
    private int cMax;
    private Context z3 = new Context();
    private int maxSols = 3;

    public static List<Term> makeAllTerms(int order) {
        Term[] terms = new Term[(order + 1) * (order + 1) * (order + 1)];
        int n = 0;
        for (int i = 0; i <= order; ++i) {
            for (int j = 0; j <= order; ++j) {
                for (int k = 0; k <= order; ++k) {
                    terms[n++] = Term.make(i, j, k);
                }
            }
        }
        assert n == terms.length;
        Arrays.sort(terms);
        return List.of(terms);
    }

    public LinearSolver(List<Term> terms, int cMax) {
        this.terms = terms;
        this.cMax = cMax;
    }

    public static final int SHORT_TIMEOUT_MS = 500;

    /**
     * For each example, compute a solution set using the linear solver.
     */
    public Map<SolutionSet, Classification> computeSolutionSets(List<Example> examples) {
        var sets = new HashMap<SolutionSet, Classification>();
        assert examples.size() > 0;
        var ungroupedExamples = new HashSet<Example>(examples);
        for (var ei : examples) {
            if (!ungroupedExamples.contains(ei)) {
                // Already dealt with this one in an earlier pass
                continue;
            }

            // Start a new group for this example
            var included = new HashSet<Example>();
            included.add(ei);
            ungroupedExamples.remove(ei);

            if (logGroups) {
                System.out.println("e = " + ei);
            }

            // Test against all other examples and include them greedily as long as the
            // whole group remains consistent
            var sess = startSession(SHORT_TIMEOUT_MS);
            sess.addEquation(ei);
            for (var ej : List.copyOf(ungroupedExamples)) {
                if (logGroups) {
                    System.out.println("  j = " + ej);
                }
                sess.addEquation(ej);
                if (sess.checkSatisfiable()) {
                    included.add(ej);
                    ungroupedExamples.remove(ej);
                } else {
                    if (logGroups) {
                        System.out.println("  UNSAT");
                        // restart the session
                        sess = startSession(SHORT_TIMEOUT_MS);
                        for (var es : included) {
                            sess.addEquation(es);
                        }
                    }
                }
            }

            // Get the actual solution models for this group
            var sols = sess.solve();
            assert !sols.isEmpty();
            if (logGroups) {
                for (var sol : sols.solutions()) {
                    System.out.println("  solution:");
                    for (var termC : sol.coefficients().entrySet()) {
                        System.out.println(
                                "    " + (termC.getValue() > 1 ? termC.getValue() + " * " : "") + termC.getKey());
                    }
                }
            }
            var excluded = new HashSet<Example>(examples);
            excluded.removeAll(included);
            sets.put(sols, new Classification(included, excluded));
            ungroupedExamples.removeAll(included);
        }
        return sets;
    }

    public SolveSession startSession(int timeoutMs) {
        return new SolveSession(timeoutMs);
    }

    public class SolveSession {
        private Solver z3Solver = z3.mkSolver();
        private IntExpr z3Zero = z3.mkInt(0);
        private IntExpr z3CMax = z3.mkInt(cMax);
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

        public boolean checkSatisfiable() {
            if (z3Status == Status.UNKNOWN) {
                z3Status = z3Solver.check();
            }
            if (z3Status == Status.SATISFIABLE) {
                return true;
            } else {
                return false;
            }
        }

        public SolutionSet solve() {
            var solutions = new HashSet<LinearSolution>();

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

            if (solutions.isEmpty()) {
                return SolutionSet.EMPTY;
            }
            return new SolutionSet(solutions);
        }

        @SuppressWarnings("unchecked")
        private void addBlockingClause(Solver solver, HashMap<Term, IntExpr> z3Coeffs,
                HashMap<Term, Integer> disallowedSolution) {
            assert z3Status == Status.SATISFIABLE;
            z3Status = Status.UNKNOWN;
            var clause = z3.mkOr(disallowedSolution.entrySet().stream()
                    .map(e -> z3.mkNot(z3.mkEq(z3Coeffs.get(e.getKey()), z3.mkInt(e.getValue()))))
                    .toArray(Expr[]::new));
            // System.out.println("Solver:\n" + solver);
            // System.out.println("\nClause: " + clause + "\n");
            z3Solver.add(clause);
        }

    }

    @Override
    public void close() {
        z3.close();
    }
}
