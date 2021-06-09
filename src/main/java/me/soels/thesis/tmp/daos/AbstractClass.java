package me.soels.thesis.tmp.daos;

import lombok.Getter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Node
@Getter
public abstract class AbstractClass {
    @Id
    private final String identifier;
    private final String humanReadableName;
    @Relationship
    private final List<DependenceRelationship> dependenceRelationships = new ArrayList<>();

    protected AbstractClass(String identifier, String humanReadableName) {
        this.identifier = identifier;
        this.humanReadableName = humanReadableName;
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
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, humanReadableName);
    }
}
