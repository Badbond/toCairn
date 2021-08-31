package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import me.soels.thesis.model.SolverConfiguration;
import me.soels.thesis.solver.metric.MetricType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Models the configuration of an evaluation.
 */
@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MOEAConfigurationDto.class, name = "moea"),
        @JsonSubTypes.Type(value = HierarchicalConfigurationDto.class, name = "hierarchical")
})
public abstract class SolverConfigurationDto {
    @NotNull
    @Size(min = 1)
    private final List<MetricType> metrics;

    protected SolverConfigurationDto(List<MetricType> metrics) {
        this.metrics = metrics;
    }

    public abstract SolverConfiguration toDao();
}
