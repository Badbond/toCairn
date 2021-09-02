package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.*;

import java.util.*;

@Node
@Getter
@Setter
public abstract class AbstractClass {
    private final String identifier;
    private final String humanReadableName;
    private final String location;
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    protected UUID id;
    @CompositeProperty(prefix = "methodsDefined")
    private Map<String, List<String>> methods = new HashMap<>(); // Method FQN -> parameter FQNs
    private Integer size;

    @Relationship("InteractsWith")
    private List<DependenceRelationship> dependenceRelationships = new ArrayList<>();

    protected AbstractClass(String identifier, String humanReadableName, String location) {
        this.identifier = identifier;
        this.humanReadableName = humanReadableName;
        this.location = location;
    }

    /**
     * Sets the dependence relationships.
     * <p>
     * This method should primarily be used by Neo4j for copying the nodes. We can not make the list immutable as we
     * don't have resolved the dependencies when initializing the instance. We could use a wither, but we will not use
     * it during analysis as we incrementally add to the modifiable list instead.
     *
     * @param dependenceRelationships the relationships to set
     */
    void setDependenceRelationships(List<DependenceRelationship> dependenceRelationships) {
        this.dependenceRelationships = dependenceRelationships;
    }

    /**
     * Sets the id of this node.
     * <p>
     * This method should primarily be used by Neo4j for copying nodes and generating the UUID.
     *
     * @param id the id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Returns whether the provided class is equal to this class.
     * <p>
     * Note that we only use our {@code id} for equality checks.
     *
     * @param o the class to check equality with
     * @return whether the given class is equal to this class
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractClass that = (AbstractClass) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identifier, humanReadableName);
    }
}
