package me.soels.tocairn.api.dtos;

import lombok.Getter;
import me.soels.tocairn.model.DataRelationship;
import me.soels.tocairn.model.DataRelationshipType;
import me.soels.tocairn.model.DependenceRelationship;

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
