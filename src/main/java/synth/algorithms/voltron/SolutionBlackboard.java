package synth.algorithms.voltron;

import java.util.*;

import synth.algorithms.ast.*;
import synth.algorithms.classify.*;
import synth.algorithms.lia.*;
import synth.core.*;

public class SolutionBlackboard {
    public Set<Example> examples;
    public List<PartialSolution> partialSolutions;
    public List<List<PartialSolution>> candidateCovers;
    public List<Discriminator> discriminators;
    public List<AstNode> candidateSolutions;

    //public int scoreProgram(AstNode program);

    /**
     * Find a subset of solution sets which (in descending order of importance)
     * 1. definitely covers all examples,
     * 2. is distinguishable with minimum exact matching, and
     * 3. minimizes the number of distinct solutions.
     */
    public Map<SolutionSet, Set<Example>> minimizeSolutionCover(Map<SolutionSet, Set<Example>> groups) {
        return groups;
    }

}
