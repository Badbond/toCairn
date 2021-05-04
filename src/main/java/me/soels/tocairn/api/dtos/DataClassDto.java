package me.soels.tocairn.api.dtos;

import lombok.Getter;
import me.soels.tocairn.model.DataClass;

/**
 * Models a DTO for exposing a {@link DataClass} node of the graph.
 */
@Getter
public class DataClassDto extends AbstractClassDto {
    public DataClassDto(DataClass clazz) {
        super(clazz);
    }
}
