package synth.algorithms.classify;

import java.util.List;

import synth.algorithms.representation.*;

public class PartialSolution {
    private ExprRepresentation solution;
    private Classification application;
    private List<Discriminator> positiveDiscriminators;
    private List<Discriminator> negativeDiscriminators;

    public ExprRepresentation solution() {
        return solution;
    }

    public Classification application() {
        return application;
    }

    public List<Discriminator> positiveDiscriminators() {
        return positiveDiscriminators;
    }

    public List<Discriminator> negativeDiscriminators() {
        return negativeDiscriminators;
    }

    public PartialSolution(ExprRepresentation solution, Classification application,
            List<Discriminator> positiveDiscriminators, List<Discriminator> negativeDiscriminators) {
        this.solution = solution;
        this.application = application;
        this.positiveDiscriminators = positiveDiscriminators;
        this.negativeDiscriminators = negativeDiscriminators;
    }
}
