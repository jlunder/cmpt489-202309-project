package synth.algorithms;

import synth.algorithms.enumeration.ProgramEnumerator;
import synth.core.Example;
import synth.core.Program;

import java.util.*;

public class DFSEnum2Synthesizer extends SynthesizerBase {
    /**
     * Synthesize a program f(x, y, z) based on examples
     *
     * @param examples a list of examples
     * @return the program or null to indicate synthesis failure
     */
    @Override
    public Program synthesize(List<Example> examples) {
        for (int h = 0; h <= 2; ++h) {
            var pe = new ProgramEnumerator(h, h, ProgramEnumerator.E_SYMBOLS, ProgramEnumerator.E_SYMBOLS,
                    ProgramEnumerator.B_SYMBOLS);

            while (pe.hasNext()) {
                var node = pe.next();
                if (validate(examples, node)) {
                    return new Program(node);
                }
            }
        }
        return null;
    }

}
