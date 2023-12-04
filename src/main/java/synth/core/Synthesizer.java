package synth.core;

import java.util.List;

public interface Synthesizer {

    public Program synthesize(List<Example> examples);

}
