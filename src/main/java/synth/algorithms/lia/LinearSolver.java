package synth.algorithms.lia;

import java.util.*;

import com.microsoft.z3.*;

import synth.core.Example;

public class LinearSolver implements AutoCloseable {
    private List<Term> terms;
    private Context z3 = new Context();
    private int maxSols = 20;

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
        return List.of(terms);
    }

    public LinearSolver(List<Term> terms) {
        this.terms = terms;
    }

    public SolutionSet solve(List<Example> examples) {
        Solver solver = z3.mkSolver();
        HashMap<Term, IntExpr> z3Coeffs = new HashMap<>();
        addEquations(solver, z3Coeffs, examples);
        addBasicTermConstraints(solver, z3Coeffs);

        var status = solver.check();
        var solMaps = new ArrayList<Map<Term, Integer>>();
        var usedTerms = new TreeSet<Term>();

        while (status == Status.SATISFIABLE && solMaps.size() < maxSols) {
            var z3Model = solver.getModel();
            var solMap = new HashMap<Term, Integer>(z3Coeffs.size());
            z3Coeffs.forEach(
                    (term, coeff) -> solMap.put(term, ((IntNum) (z3Model.getConstInterp(coeff))).getInt()));
            usedTerms.addAll(solMap.keySet());
            solMaps.add(solMap);
            // That was a nice solution, let's try something different
            addBlockingClause(solver, z3Coeffs, solMap);
            status = solver.check();
        }

        if (!solMaps.isEmpty()) {
            Term[] terms = usedTerms.toArray(Term[]::new);
            int[][] sols = new int[solMaps.size()][terms.length];
            int i = 0;
            for (var s : solMaps) {
                for (int j = 0; j < terms.length; ++j) {
                    var t = terms[j];
                    sols[i][j] = s.get(t);
                }
                ++i;
            }
            return new SolutionSet(terms, sols);
        }
        return SolutionSet.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private void addEquations(Solver solver, HashMap<Term, IntExpr> z3Coeffs, List<Example> examples) {
        ArrayList<ArithExpr<IntSort>> z3Terms = new ArrayList<>();
        for (var e : examples) {
            z3Terms.clear();
            for (var term : terms) {
                var c = z3Coeffs.computeIfAbsent(term, t -> z3.mkIntConst(t.name()));
                z3Terms.add(z3.mkMul(z3.mkInt(term.evalTerm(e.input())), c));
            }
            var sum = z3.mkAdd(z3Terms.toArray(ArithExpr[]::new));
            var eqn = z3.mkEq(sum, z3.mkInt(e.output()));
            solver.add(eqn);
        }
    }

    @SuppressWarnings("unchecked")
    private void addBasicTermConstraints(Solver solver, HashMap<Term, IntExpr> z3Coeffs) {
        var z3Zero = z3.mkInt(0);
        for (var c : z3Coeffs.values()) {
            var eqn = z3.mkGe(c, z3Zero);
            solver.add(eqn);
        }
    }

    @SuppressWarnings("unchecked")
    private void addBlockingClause(Solver solver, HashMap<Term, IntExpr> z3Coeffs,
            HashMap<Term, Integer> disallowedSolution) {
        var clause = z3.mkOr(disallowedSolution.entrySet().stream()
                .map(e -> z3.mkNot(z3.mkEq(z3Coeffs.get(e.getKey()), z3.mkInt(e.getValue())))).toArray(Expr[]::new));
        // System.out.println("Solver:\n" + solver);
        // System.out.println("\nClause: " + clause + "\n");
        solver.add(clause);
    }

    @Override
    public void close() {
        z3.close();
    }
}
