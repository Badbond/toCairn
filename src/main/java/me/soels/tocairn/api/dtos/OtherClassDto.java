package me.soels.tocairn.api.dtos;

import lombok.Getter;
import me.soels.tocairn.model.OtherClass;

/**
 * Models a DTO for exposing an {@link OtherClass} node of the graph.
 */
@Getter
public class OtherClassDto extends AbstractClassDto {
    public OtherClassDto(OtherClass clazz) {
        super(clazz);
    }
}
