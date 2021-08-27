package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import me.soels.thesis.model.SolverConfiguration;

/**
 * Models the configuration of an evaluation.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MOEAConfigurationDto.class, name = "moea"),
        @JsonSubTypes.Type(value = HierarchicalConfigurationDto.class, name = "hierarchical")
})
public abstract class SolverConfigurationDto {
    public abstract SolverConfiguration toDao();
}
