package me.soels.tocairn.api.dtos;

import lombok.Getter;
import me.soels.tocairn.model.DependenceRelationship;

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
