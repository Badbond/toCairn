package me.soels.thesis.tmp.dtos;

import lombok.Getter;
import me.soels.thesis.model.OtherClass;

/**
 * Models a DTO for exposing an {@link OtherClass} node of the graph.
 */
@Getter
public class OtherClassDto extends AbstractClassDto {
    public OtherClassDto(OtherClass clazz) {
        super(clazz);
    }
}
