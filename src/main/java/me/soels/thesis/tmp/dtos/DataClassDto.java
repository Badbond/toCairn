package me.soels.thesis.tmp.dtos;

import lombok.Getter;
import me.soels.thesis.tmp.daos.DataClass;

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
