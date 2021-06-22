package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.DependenceRelationship;

/**
 * Models a DTO for exposing an {@link DependenceRelationship} node of the graph.
 */
@Getter
public abstract class AbstractRelationshipDto {
    private final String callerFqn;
    private final String calleeFqn;

    protected AbstractRelationshipDto(String callerFqn, String calleeFqn) {
        this.callerFqn = callerFqn;
        this.calleeFqn = calleeFqn;
    }
}
