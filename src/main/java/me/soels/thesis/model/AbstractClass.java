package me.soels.thesis.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;

@Node
public abstract class AbstractClass {
    @Id
    private final String identifier;
    private final String humanReadableName;

    // TODO: We require @Relationship here. We can then use @RelationshipProperties on DependenceRelationship and
    //  @TargetNode on the callee.

    protected AbstractClass(String identifier, String humanReadableName) {
        this.identifier = identifier;
        this.humanReadableName = humanReadableName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    /**
     * Returns whether the provided class is equal to this class.
     * <p>
     * Note that we only use our {@code identifier} for equality checks.
     *
     * @param o the class to check equality with
     * @return whether the given class is equal to this class
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractClass that = (AbstractClass) o;
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, humanReadableName);
    }
}
