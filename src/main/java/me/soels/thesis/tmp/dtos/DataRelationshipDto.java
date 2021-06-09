package me.soels.thesis.tmp.dtos;

import lombok.Getter;
import me.soels.thesis.tmp.daos.DataRelationshipType;
import me.soels.thesis.tmp.daos.DependenceRelationship;

/**
 * Models a DTO for exposing an {@link DependenceRelationship} node of the graph.
 */
@Getter
public class DataRelationshipDto extends DependenceRelationshipDto {
    private final DataRelationshipType type;

    public DataRelationshipDto(String calleeFqn, String callerFqn, int frequency, DataRelationshipType type) {
        super(calleeFqn, callerFqn, frequency);
        this.type = type;
    }
}
