package synth;

import synth.algorithms.*;
import synth.core.*;
import synth.dsl.Semantics;
import synth.util.*;

import java.util.*;
import java.util.logging.*;

public class Main {
    private static Logger logger;

    static {
        Properties props = System.getProperties();
        props.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT.%1$tL] %4$s: %5$s%n");

        // Logger.getLogger("").addHandler(new ConsoleHandler());
        Logger.getLogger("").setLevel(Level.WARNING);
        Logger.getLogger("synth.Main").setLevel(Level.ALL);
        // Logger.getLogger("synth.algorithms.lia.LinearSolver").setLevel(Level.ALL);
        // Logger.getLogger("synth.algorithms.lia.ORToolsCPLinearSolver").setLevel(Level.ALL);
        //Logger.getLogger("synth.algorithms").setLevel(Level.ALL);

        logger = Logger.getLogger("synth.Main");
    }

    public static void main(String[] args) {

        // String examplesFilePath = "examples.txt";

        var synthesizers = List.of(
                new BFSEnum2Synthesizer(),
                new VoltronSynthesizer(),
                new Mcmc1Synthesizer());
        // Synthesizer synthesizer = new MultiStrategySynthesizer();

        for (var examplesFilePath : args) {
            if (args.length > 1) {
                System.out.println("# " + examplesFilePath);
            }
            List<String> lines = FileUtils.readLinesFromFile(examplesFilePath);

            // parse all examples
            List<Example> examples = Parser.parseAllExamples(lines);

            // run the synthesizers
            Program program = null;
            for (var synthesizer : synthesizers) {
                logger.info(String.format("Attempting solution with %s", synthesizer.getClass().getSimpleName()));
                program = synthesizer.synthesize(examples);
                if (program != null) {
                    for (var e : examples) {
                        var evalOutput = Semantics.evaluate(program, e.input());
                        if (evalOutput != e.output()) {
                            logger.log(Level.WARNING, "Synthesizer generated bad program: {0}",
                                    new Object[] { program });
                            logger.log(Level.WARNING, "Failed on example: {0}; produced {1}",
                                    new Object[] { e, evalOutput });
                            program = null;
                            break;
                        }
                    }
                }
                if (program != null) {
                    break;
                }
            }
            if (program == null) {
                System.out.println("error: failed to generate!");
            } else {
                System.out.println(program);
            }
        }
    }
}
