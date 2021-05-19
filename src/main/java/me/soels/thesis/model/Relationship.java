package me.soels.thesis.model;

/**
 * Indicates a relationships between two classes.
 *
 * @see DataRelationship
 * @see DependenceRelationship
 */
public abstract class Relationship {
    private final AbstractClass caller;
    private final AbstractClass callee;

    protected Relationship(AbstractClass caller, AbstractClass callee) {
        this.caller = caller;
        this.callee = callee;
    }

    public AbstractClass getCaller() {
        return caller;
    }

    public AbstractClass getCallee() {
        return callee;
    }
}
