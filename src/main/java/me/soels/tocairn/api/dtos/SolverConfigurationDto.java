package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import me.soels.tocairn.model.SolverConfiguration;
import me.soels.tocairn.solver.metric.MetricType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Optional;

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

    @Size(min = 1)
    private final Integer minClusterAmount;

    @Size(min = 1)
    private final Integer maxClusterAmount;

    protected SolverConfigurationDto(SolverConfiguration dao) {
        this.metrics = dao.getMetrics();
        this.minClusterAmount = dao.getMinClusterAmount().orElse(null);
        this.maxClusterAmount = dao.getMaxClusterAmount().orElse(null);
    }

    protected SolverConfigurationDto(List<MetricType> metrics, Integer minClusterAmount, Integer maxClusterAmount) {
        this.metrics = metrics;
        this.minClusterAmount = minClusterAmount;
        this.maxClusterAmount = maxClusterAmount;
    }

    public Optional<Integer> getMinClusterAmount() {
        return Optional.ofNullable(minClusterAmount);
    }

    public Optional<Integer> getMaxClusterAmount() {
        return Optional.ofNullable(maxClusterAmount);
    }

    public abstract SolverConfiguration toDao();
}
