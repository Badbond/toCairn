package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.DataClass;

/**
 * Models a DTO for exposing a {@link DataClass} node of the graph.
 */
@Getter
public class DataClassDto extends AbstractClassDto {
    private final int size;

    public DataClassDto(DataClass clazz) {
        super(clazz);
        this.size = clazz.getSize();
    }
}