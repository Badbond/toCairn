package me.soels.thesis.tmp.dtos;

import lombok.Getter;
import me.soels.thesis.model.DependenceRelationship;

/**
 * Models a DTO for exposing an {@link DependenceRelationship} node of the graph.
 */
@Getter
public abstract class AbstractRelationshipDto {
    private final String calleeFqn;
    private final String callerFqn;

    protected AbstractRelationshipDto(String calleeFqn, String callerFqn) {
        this.calleeFqn = calleeFqn;
        this.callerFqn = callerFqn;
    }
}
