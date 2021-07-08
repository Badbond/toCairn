package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.DataRelationship;
import me.soels.thesis.model.DataRelationshipType;
import me.soels.thesis.model.DependenceRelationship;

/**
 * Models a DTO for exposing an {@link DependenceRelationship} node of the graph.
 */
@Getter
public class DataRelationshipDto extends DependenceRelationshipDto {
    private final DataRelationshipType type;

    public DataRelationshipDto(String callerFqn, DataRelationship relationship) {
        super(callerFqn, relationship);
        this.type = relationship.getType();
    }
}
