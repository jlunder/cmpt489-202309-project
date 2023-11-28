package synth.algorithms.lia;

import java.util.*;

import com.microsoft.z3.*;

public class LinearSolver {
    public SolutionSpace solve(Equation[] system) {
        var z3 = new Context();
        try {
            var z3Coeffs = new HashMap<String, IntExpr>();
            var solver = z3.mkSolver();
            ArrayList<ArithExpr> z3Terms = new ArrayList<>();
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
            var status = solver.check();
            var solMaps = new ArrayList<Map<String, Integer>>();
            var usedTerms = new TreeSet<String>();
            while (status == Status.SATISFIABLE) {
                var z3Model = solver.getModel();
                var solMap = new HashMap<String, Integer>(z3Coeffs.size());
                z3Coeffs.forEach(
                        (name, coeff) -> solMap.put(name, ((IntNum) (z3Model.getConstInterp(coeff))).getInt()));
                usedTerms.addAll(solMap.keySet());
                solMaps.add(solMap);
                break;
            }
            if(!solMaps.isEmpty()) {
                String[] terms = usedTerms.toArray(String[]::new);
                int[][] sols = new int[solMaps.size()][terms.length];
                int i = 0;
                for (var s : solMaps) {
                    for (int j = 0; j < terms.length; ++j) {
                        var t = terms[i];
                        sols[i][j] = s.get(t);
                    }
                    ++i;
                }
                return new SolutionSpace(terms, sols);
            }
            return SolutionSpace.EMPTY;
        } finally {
            z3.close();
        }
    }
}
