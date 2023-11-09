package synth;

import synth.algorithms.*;
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

            // run the synthesizer
            ISynthesizer synthesizer = new DFSEnum1Synthesizer();
            Program program = synthesizer.synthesize(examples);
            if (program == null) {
                System.out.println("error: failed to generate!");
            } else {
                System.out.println(program);
            }
        }
    }
}
