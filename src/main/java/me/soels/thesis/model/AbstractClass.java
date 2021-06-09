package me.soels.thesis.model;

import lombok.Getter;
import org.neo4j.
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;
import java.util.UUID;

@Node
@Getter
public abstract class AbstractClass {
    // TODO: Change the ID to a random UUID. There should be a unique
    @Id
    private final String identifier;
    private final String humanReadableName;
    private final UUID evaluationId;

    // TODO: We require @Relationship here. We can then use @RelationshipProperties on DependenceRelationship and
    //  @TargetNode on the callee.

    protected AbstractClass(String identifier, String humanReadableName, UUID evaluationId) {
        this.identifier = identifier;
        this.humanReadableName = humanReadableName;
        this.evaluationId = evaluationId;
    }

    /**
     * Returns whether the provided class is equal to this class.
     * <p>
     * Note that we only use our {@code identifier} and {@code evaluationId} for equality checks.
     *
     * @param o the class to check equality with
     * @return whether the given class is equal to this class
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractClass that = (AbstractClass) o;
        return identifier.equals(that.identifier) && evaluationId.equals(that.evaluationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, humanReadableName, evaluationId);
    }
}
