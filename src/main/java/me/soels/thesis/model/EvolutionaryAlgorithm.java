package me.soels.thesis.model;

/**
 * Indicates which algorithm should be used for the evaluation.
 */
public enum EvolutionaryAlgorithm {
    NSGA_II("NSGAII");

    private final String name;

    EvolutionaryAlgorithm(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
