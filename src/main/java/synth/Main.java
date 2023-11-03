package synth;

import synth.cfg.*;
import synth.core.*;
import synth.util.*;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // String examplesFilePath = "examples.txt";
        String examplesFilePath = args[0];
        List<String> lines = FileUtils.readLinesFromFile(examplesFilePath);
        // parse all examples
        List<Example> examples = Parser.parseAllExamples(lines);
        // read the CFG
        CFG cfg = buildCFG();
        ISynthesizer synthesizer = new TopDownEnumSynthesizer();
        Program program = synthesizer.synthesize(cfg, examples);
        System.out.println(program);
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
        NonTerminal startSymbol = new NonTerminal("E");
        Map<NonTerminal, List<Production>> symbolToProductions = new HashMap<>();
        {
            NonTerminal retSymbol = new NonTerminal("E");
            List<Production> prods = List.of(
                    new Production(new NonTerminal("E"), new Terminal("1"), Collections.emptyList()),
                    new Production(new NonTerminal("E"), new Terminal("2"), Collections.emptyList()),
                    new Production(new NonTerminal("E"), new Terminal("3"), Collections.emptyList()),
                    new Production(new NonTerminal("E"), new Terminal("x"), Collections.emptyList()),
                    new Production(new NonTerminal("E"), new Terminal("y"), Collections.emptyList()),
                    new Production(new NonTerminal("E"), new Terminal("z"), Collections.emptyList()),
                    new Production(new NonTerminal("E"), new Terminal("Ite"),
                            List.of(new NonTerminal("B"), new NonTerminal("E"), new NonTerminal("E"))),
                    new Production(new NonTerminal("E"), new Terminal("Add"),
                            List.of(new NonTerminal("E"), new NonTerminal("E"))),
                    new Production(new NonTerminal("E"), new Terminal("Multiply"),
                            List.of(new NonTerminal("E"), new NonTerminal("E"))));
            symbolToProductions.put(retSymbol, prods);
        }
        {
            NonTerminal retSymbol = new NonTerminal("B");
            List<Production> prods = List.of(
                    new Production(new NonTerminal("B"), new Terminal("Lt"),
                            List.of(new NonTerminal("E"), new NonTerminal("E"))),
                    new Production(new NonTerminal("B"), new Terminal("Eq"),
                            List.of(new NonTerminal("E"), new NonTerminal("E"))),
                    new Production(new NonTerminal("B"), new Terminal("And"),
                            List.of(new NonTerminal("B"), new NonTerminal("B"))),
                    new Production(new NonTerminal("B"), new Terminal("Or"),
                            List.of(new NonTerminal("B"), new NonTerminal("B"))),
                    new Production(new NonTerminal("B"), new Terminal("Not"), List.of(new NonTerminal("B"))));
            symbolToProductions.put(retSymbol, prods);
        }
        return new CFG(startSymbol, symbolToProductions);
    }
}
