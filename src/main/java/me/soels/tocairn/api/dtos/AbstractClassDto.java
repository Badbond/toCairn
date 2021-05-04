package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import me.soels.tocairn.model.AbstractClass;

/**
 * Models a DTO for exposing an {@link AbstractClass} node of the graph.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DataClassDto.class, name = "data"),
        @JsonSubTypes.Type(value = OtherClassDto.class, name = "other")
})
@Getter
public abstract class AbstractClassDto {
    private final String fqn;
    private final String humanReadableName;

    protected AbstractClassDto(AbstractClass clazz) {
        this.fqn = clazz.getIdentifier();
        this.humanReadableName = clazz.getHumanReadableName();
    }
}
