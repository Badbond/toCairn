package me.soels.tocairn.model;

import lombok.Getter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Node
@Getter
public final class OtherClass extends AbstractClass {
    @Relationship("DataDepends")
    private List<DataRelationship> dataRelationships = new ArrayList<>();
    private final int methodCount;
    private final boolean isExecutedAPIClass;

    public OtherClass(String identifier, String humanReadableName, String location, Set<String> features, int methodCount, boolean isExecutedAPIClass) {
        super(identifier, humanReadableName, location, features);
        this.methodCount = methodCount;
        this.isExecutedAPIClass = isExecutedAPIClass;
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
