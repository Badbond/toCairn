package me.soels.thesis.model;

import java.util.UUID;

/**
 * Indicates a relationships between two classes.
 *
 * @see DataRelationship
 * @see DependenceRelationship
 */
public abstract class Relationship {
    // TODO: Create an @Id for the graph
    private final UUID evaluationId;
    private final AbstractClass callee;

    protected Relationship(UUID evaluationId, AbstractClass callee) {
        this.evaluationId = evaluationId;
        this.callee = callee;
    }

    public AbstractClass getCallee() {
        return callee;
    }
}
