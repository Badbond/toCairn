package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.DependenceRelationship;

/**
 * Models a DTO for exposing an {@link DependenceRelationship} node of the graph.
 */
@Getter
public class DependenceRelationshipDto extends AbstractRelationshipDto {
    private final int staticFrequency;
    private final Integer dynamicFrequency;

    public DependenceRelationshipDto(String callerFqn, DependenceRelationship dependenceRelationship) {
        super(callerFqn, dependenceRelationship.getCallee().getIdentifier());
        this.staticFrequency = dependenceRelationship.getStaticFrequency();
        this.dynamicFrequency = dependenceRelationship.getDynamicFrequency().orElse(null);
    }
}
