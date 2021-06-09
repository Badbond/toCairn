package me.soels.thesis.tmp.daos;

import lombok.Getter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node
@Getter
public final class OtherClass extends AbstractClass {
    @Relationship
    private final List<DataRelationship> dataRelationships = new ArrayList<>();

    public OtherClass(String identifier, String humanReadableName) {
        super(identifier, humanReadableName);
    }
}
