package me.soels.thesis.model;

/**
 * Relationship between two {@link OtherClass} that signify one depends on the other.
 * <p>
 * This relationship is used to identify non-data relationships in our graph. The {@link #getCount()} is the number
 * of times that {@link #getCaller()} calls {@link #getCallee()} in a different location of the source code. Therefore,
 * this is constructed using static analysis. We don't use dynamic analysis as that is too costly to run for all
 * classes and methods.
 */
public final class DependenceRelationship extends Relationship {
    private final int count;

    public DependenceRelationship(OtherClass caller, OtherClass callee, int count) {
        super(caller, callee);
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    @Override
    public OtherClass getCaller() {
        return (OtherClass) super.getCaller();
    }

    @Override
    public OtherClass getCallee() {
        return (OtherClass) super.getCallee();
    }
}
