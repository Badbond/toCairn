package me.soels.thesis.model;

/**
 * Indicates a relationships between two classes.
 *
 * @see DataRelationship
 * @see DependenceRelationship
 */
public abstract class Relationship {
    private final AbstractClass callee;

    protected Relationship(AbstractClass callee) {
        this.callee = callee;
    }

    public AbstractClass getCallee() {
        return callee;
    }
}
