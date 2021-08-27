package me.soels.thesis.model;

import lombok.Getter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Node
@Getter
public final class OtherClass extends AbstractClass {
    @Relationship("DataDepends")
    private List<DataRelationship> dataRelationships = new ArrayList<>();

    public OtherClass(String identifier, String humanReadableName) {
        super(identifier, humanReadableName);
    }

    /**
     * Sets the relationships to data objects.
     * <p>
     * This method should primarily be used by Neo4j for copying the nodes. We can not make the list immutable as we
     * don't have resolved the dependencies when initializing the instance. We could use a wither, but we will not use
     * it during analysis as we incrementally add to the modifiable list instead.
     *
     * @param dataRelationships the relationships to set
     */
    public void setDataRelationships(List<DataRelationship> dataRelationships) {
        this.dataRelationships = dataRelationships;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataRelationships);
    }
}
