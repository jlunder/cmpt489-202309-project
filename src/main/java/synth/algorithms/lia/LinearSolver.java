package synth.algorithms.lia;

import java.util.*;

import com.microsoft.z3.*;

import synth.algorithms.classify.Classification;
import synth.core.Example;

public class LinearSolver implements AutoCloseable {
    public static class SolveResult {
        private boolean solutionsAreDefinite;
        private List<LinearSolution> solutions;

        public boolean solutionsAreDefinite() {
            return solutionsAreDefinite;
        }

        public List<LinearSolution> solutions() {
            return solutions;
        }

        SolveResult(boolean hasDefiniteSolutions, List<LinearSolution> solutions) {
            this.solutionsAreDefinite = hasDefiniteSolutions;
            this.solutions = solutions;
        }
    }

    private static final boolean logGroups = true;

    private List<Term> terms;
    private int cMax;
    private Context z3 = new Context();
    private int maxSols = 5;
    private Random rng = new Random();

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

    public static final int SHORT_TIMEOUT_MS = 5000;
    //public static final int SHORT_TIMEOUT_MS = 500;

    private ArrayList<Example> randomOrder(Collection<Example> examples) {
        var scrambled = new ArrayList<Example>(examples);
        var len = examples.size();
        for (int i = 0; i < len; ++i) {
            var j = rng.nextInt(len);
            var tmp = scrambled.get(j);
            scrambled.set(j, scrambled.get(i));
            scrambled.set(i, tmp);
        }
        return scrambled;
    }

    /**
     * For each example, compute a solution set using the linear solver.
     */
    public Map<LinearSolution, Classification> computeSolutionSets(List<Example> examples) {
        assert examples.size() > 0;

        // avoid accidental or malicious bias
        var scrambledExamples = randomOrder(examples);

        var sets = new HashMap<LinearSolution, Classification>();
        var ungroupedExamples = new HashSet<Example>(examples);

        for (var ei : scrambledExamples) {
            if (!ungroupedExamples.contains(ei)) {
                // Already dealt with this one in an earlier pass
                continue;
            }

            // Start a new group for this example
            var included = new HashSet<Example>();
            included.add(ei);

            if (logGroups) {
                System.out.println("e = " + ei);
            }

            var excluded = new HashSet<Example>(examples);
            var res = findMoreGroupMembersWithZ3(excluded, included);
            excluded.removeAll(included);
            sets.put(res.solutions().iterator().next(), new Classification(included, excluded));
            ungroupedExamples.removeAll(included);
        }

        assert ungroupedExamples.isEmpty();
        return sets;
    }

    private SolveResult findMoreGroupMembersWithZ3(HashSet<Example> ungroupedExamples, HashSet<Example> included) {
        var scrambledExamples = randomOrder(ungroupedExamples);
        assert included.size() == 1;
        // Test against all other examples, adding more until there's enough in the
        // group for it to have a definite solution
        SolveResult res = null;

        ungroupedExamples.removeAll(included);

        for (var ej : List.copyOf(scrambledExamples)) {
            if (logGroups) {
                System.out.println("  j = " + ej);
            }

            // Set up the Z3 solve session
            var sess = startSession(SHORT_TIMEOUT_MS);
            for (var ei : included) {
                sess.addEquation(ei);
            }
            sess.addEquation(ej);

            if (sess.checkSatisfiable()) {
                included.add(ej);
                ungroupedExamples.remove(ej);
                res = sess.solve();
                if (res.solutionsAreDefinite()) {
                    return completeGroupUsingSolutions(res.solutions(), ungroupedExamples, included);
                }
            } else {
                if (logGroups) {
                    System.out.println("  UNSAT");
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
        // solution
        var ex = included.iterator().next();
        if (ex.output() > 0) {
            return new SolveResult(false, List.of(new LinearSolution(Map.of(Term.TERM_1, ex.output()))));
        } else {
            var sess = startSession(SHORT_TIMEOUT_MS);
            sess.addEquation(ex);
            return sess.solve();
        }
    }

    private SolveResult completeGroupUsingSolutions(List<LinearSolution> solutions, HashSet<Example> ungroupedExamples,
            HashSet<Example> included) {
        if (logGroups) {
            System.out.println(String.format("  Completing using %d solutions (%d grouped, %d remain)",
                    solutions.size(), included.size(), ungroupedExamples.size()));
        }
        var viableSolutions = new HashSet<LinearSolution>(solutions);
        for (var e : List.copyOf(ungroupedExamples)) {
            boolean any = false, all = true;
            for (var s : viableSolutions) {
                var compatible = (s.evalExpr(e.input()) == e.output());
                any |= compatible;
                all &= compatible;
            }
            if (any) {
                included.add(e);
                ungroupedExamples.remove(e);
                if (!all) {
                    for (var s : List.copyOf(viableSolutions)) {
                        if (s.evalExpr(e.input()) != e.output()) {
                            viableSolutions.remove(s);
                        }
                    }
                }
            }
        }
        if (logGroups) {
            System.out.println(String.format("  Done: winnowed to %d solutions (%d grouped, %d remain)",
                    viableSolutions.size(), included.size(), ungroupedExamples.size()));
        }
        return new SolveResult(true, List.copyOf(viableSolutions));
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

        public SolveResult solve() {
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
