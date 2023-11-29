package synth.algorithms.lia;

import java.util.*;

import com.microsoft.z3.*;

public class LinearSolver implements AutoCloseable {
    private HashMap<String, IntExpr> z3Coeffs = new HashMap<>();
    private Context z3 = new Context();
    private int maxSols = 20;

    public SolutionSpace solve(Equation[] system) {
        Solver solver = z3.mkSolver();
        addEquations(solver, system);
        addBasicTermConstraints(solver);

        var status = solver.check();
        var solMaps = new ArrayList<Map<String, Integer>>();
        var usedTerms = new TreeSet<String>();

        while (status == Status.SATISFIABLE && solMaps.size() < maxSols) {
            var z3Model = solver.getModel();
            var solMap = new HashMap<String, Integer>(z3Coeffs.size());
            z3Coeffs.forEach(
                    (name, coeff) -> solMap.put(name, ((IntNum) (z3Model.getConstInterp(coeff))).getInt()));
            usedTerms.addAll(solMap.keySet());
            solMaps.add(solMap);
            // That was a nice solution, let's try something different
            addBlockingClause(solver, solMap);
            status = solver.check();
        }

        if (!solMaps.isEmpty()) {
            String[] terms = usedTerms.toArray(String[]::new);
            int[][] sols = new int[solMaps.size()][terms.length];
            int i = 0;
            for (var s : solMaps) {
                for (int j = 0; j < terms.length; ++j) {
                    var t = terms[j];
                    sols[i][j] = s.get(t);
                }
                ++i;
            }
            return new SolutionSpace(terms, sols);
        }
        return SolutionSpace.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private void addEquations(Solver solver, Equation[] system) {
        ArrayList<ArithExpr<IntSort>> z3Terms = new ArrayList<>();
        for (var e : system) {
            z3Terms.clear();
            for (var t : e.terms()) {
                var c = z3Coeffs.computeIfAbsent(t.name(), name -> z3.mkIntConst(name));
                z3Terms.add(z3.mkMul(z3.mkInt(t.value()), c));
            }
            var sum = z3.mkAdd(z3Terms.toArray(ArithExpr[]::new));
            var eqn = z3.mkEq(sum, z3.mkInt(e.equalTo()));
            solver.add(eqn);
        }
    }

    @SuppressWarnings("unchecked")
    private void addBasicTermConstraints(Solver solver) {
        var z3Zero = z3.mkInt(0);
        for (var c : z3Coeffs.values()) {
            var eqn = z3.mkGe(c, z3Zero);
            solver.add(eqn);
        }
    }

    @SuppressWarnings("unchecked")
    private void addBlockingClause(Solver solver, HashMap<String, Integer> disallowedSolution) {
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
