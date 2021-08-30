package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.thesis.model.HierarchicalConfiguration;
import me.soels.thesis.solver.metric.MetricType;

import java.util.List;
import java.util.Set;


@Getter
public class HierarchicalConfigurationDto extends SolverConfigurationDto {
    private final List<Double> weights;
    // TODO: Add configuration fields for Hierarchical clustering algorithm.

    public HierarchicalConfigurationDto(HierarchicalConfiguration dao) {
        super(dao.getMetrics());
        this.weights = dao.getWeights();
    }

    @JsonCreator
    public HierarchicalConfigurationDto(Set<MetricType> metrics, List<Double> weights) {
        super(metrics);
        this.weights = weights;
    }

    @Override
    public HierarchicalConfiguration toDao() {
        var dao = new HierarchicalConfiguration();
        dao.getMetrics().addAll(getMetrics());
        dao.getWeights().addAll(weights);
        return dao;
    }
}
