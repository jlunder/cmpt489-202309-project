package synth.util;

import synth.core.Environment;
import synth.core.Example;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private record VarValuePair(String varName, int value) {
    }

    /**
     * Parse one example from a string.
     * 
     * @param text example text of the form x=a, y=b, z=c -> d, where a, b, c, d are
     *             integers.
     *             Note that the equal sign "=", comma ",", and right arrow "->" are
     *             hard coded.
     *             Also note that the variable names are case-sensitive, and we use
     *             lower case x, y, z.
     * @return the example
     */
    public static Example parseAnExample(String text) {
        String[] tokens = text.split("->");
        assert tokens.length == 2 : "Parsing error in line " + text;
        String[] pairs = tokens[0].trim().split(",");
        int x = 0, y = 0, z = 0;
        for (String pair : pairs) {
            var vv = parseVarValuePair(pair);
            if (vv.varName().equals("x")) {
                x = vv.value();
            } else if (vv.varName().equals("y")) {
                y = vv.value();
            } else if (vv.varName().equals("z")) {
                z = vv.value();
            }
        }
        Environment input = new Environment(x, y, z);
        int output = Integer.parseInt(tokens[1].trim());
        return new Example(input, output);
    }

    /**
     * Parse a list of examples from a list of strings, ignoring empty lines.
     * 
     * @param lines a list of example strings
     * @return a list of examples
     */
    public static List<Example> parseAllExamples(List<String> lines) {
        List<Example> examples = new ArrayList<>();
        for (String line : lines) {
            if (!line.isEmpty() && !line.trim().startsWith("#")) {
                examples.add(parseAnExample(line));
            }
        }
        return examples;
    }

    /**
     * Parse a pair of variable name and value.
     * 
     * @param text pair of the form x=a
     * @return a singleton map from the variable name to the value
     */
    private static VarValuePair parseVarValuePair(String text) {
        String[] tokens = text.split("=");
        assert tokens.length == 2 : "Parsing error in pair " + text;
        String varName = tokens[0].trim();
        String valueText = tokens[1].trim();
        return new VarValuePair(varName, Integer.parseInt(valueText));
    }
}
