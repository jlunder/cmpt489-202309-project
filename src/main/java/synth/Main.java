package synth;

import synth.algorithms.*;
import synth.cfg.*;
import synth.core.*;
import synth.util.*;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // String examplesFilePath = "examples.txt";
        for (var examplesFilePath : args) {
            if (args.length > 1) {
                System.out.println("# " + examplesFilePath);
            }
            List<String> lines = FileUtils.readLinesFromFile(examplesFilePath);
            // parse all examples
            List<Example> examples = Parser.parseAllExamples(lines);
            // read the CFG
            CFG cfg = buildCFG();
            ISynthesizer synthesizer = new BFSEnum1Synthesizer();
            Program program = synthesizer.synthesize(cfg, examples);
            if (program == null) {
                System.out.println("error: failed to generate!");
            } else {
                System.out.println(program);
            }
        }
    }

    /**
     * Build the following context-free grammar (CFG):
     * E ::= Ite(B, E, E) | Add(E, E) | Multiply(E, E) | x | y | z | 1 | 2 | 3
     * B ::= Lt(E, E) | Eq(E, E) | And(B, B) | Or(B, B) | Not(B)
     * where x, y, z are variables. 1, 2, 3 are constants. Lt means "less than". Eq
     * means "equals"
     *
     * @return the CFG
     */
    private static CFG buildCFG() {
        Map<Symbol, List<Production>> symbolToProductions = new HashMap<>();
        {
            List<Production> prods = List.of(
                    new Production(Symbol.E, Symbol.Const1, Collections.emptyList()),
                    new Production(Symbol.E, Symbol.Const2, Collections.emptyList()),
                    new Production(Symbol.E, Symbol.Const3, Collections.emptyList()),
                    new Production(Symbol.E, Symbol.VarX, Collections.emptyList()),
                    new Production(Symbol.E, Symbol.VarY, Collections.emptyList()),
                    new Production(Symbol.E, Symbol.VarZ, Collections.emptyList()),
                    new Production(Symbol.E, Symbol.Ite, List.of(Symbol.B, Symbol.E, Symbol.E)),
                    new Production(Symbol.E, Symbol.Add, List.of(Symbol.E, Symbol.E)),
                    new Production(Symbol.E, Symbol.Multiply, List.of(Symbol.E, Symbol.E)));
            symbolToProductions.put(Symbol.E, prods);
        }
        {
            List<Production> prods = List.of(
                    new Production(Symbol.B, Symbol.Lt,  List.of(Symbol.E, Symbol.E)),
                    new Production(Symbol.B, Symbol.Eq,  List.of(Symbol.E, Symbol.E)),
                    new Production(Symbol.B, Symbol.And, List.of(Symbol.B, Symbol.B)),
                    new Production(Symbol.B, Symbol.Or,  List.of(Symbol.B, Symbol.B)),
                    new Production(Symbol.B, Symbol.Not, List.of(Symbol.B)));
            symbolToProductions.put(Symbol.B, prods);
        }
        return new CFG(Symbol.E, symbolToProductions);
    }
}
