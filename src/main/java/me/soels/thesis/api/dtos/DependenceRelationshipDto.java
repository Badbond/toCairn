package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.DependenceRelationship;

/**
 * Models a DTO for exposing an {@link DependenceRelationship} node of the graph.
 */
@Getter
public class DependenceRelationshipDto extends AbstractRelationshipDto {
    private final int frequency;

    public DependenceRelationshipDto(String calleeFqn, String callerFqn, int frequency) {
        super(calleeFqn, callerFqn);
        this.frequency = frequency;
    }
}
