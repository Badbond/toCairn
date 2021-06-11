package me.soels.thesis.model;

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

    // TODO: Monday investigate error 'Cannot set property dependenceRelationships because no setter, no wither and it's not part of the persistence constructor public me.soels.thesis.model.OtherClass(java.lang.String,java.lang.String)!'
    public OtherClass(String identifier, String humanReadableName) {
        super(identifier, humanReadableName);
    }
}
