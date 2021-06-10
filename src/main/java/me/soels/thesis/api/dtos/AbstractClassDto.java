package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.AbstractClass;

/**
 * Models a DTO for exposing an {@link AbstractClass} node of the graph.
 */
@Getter
public abstract class AbstractClassDto {
    private final String fqn;
    private final String humanReadableName;

    protected AbstractClassDto(AbstractClass clazz) {
        this.fqn = clazz.getIdentifier();
        this.humanReadableName = clazz.getHumanReadableName();
    }
}
