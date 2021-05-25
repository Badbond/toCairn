package me.soels.thesis.model;

/**
 * Relationship between two {@link OtherClass} that signify one depends on the other.
 * <p>
 * This relationship is used to identify non-data relationships in our graph. The {@link #getFrequency()} is the number
 * of times that {@link #getCaller()} calls {@link #getCallee()} in a different location of the source code. Therefore,
 * this is constructed using static analysis. We don't use dynamic analysis as that is too costly to run for all
 * classes and methods.
 * TODO: Change JavaDoc, now all relationships are dependence relationships but data relationships are more niche.
 */
public class DependenceRelationship extends Relationship {
    // TODO: List of methods invoked? count can be deduced from that. Not sure if needed but we can add it. Also for other dep.
    private final int frequency;

    public DependenceRelationship(AbstractClass caller, AbstractClass callee, int frequency) {
        super(caller, callee);
        this.frequency = frequency;
    }

    public int getFrequency() {
        return frequency;
    }
}
