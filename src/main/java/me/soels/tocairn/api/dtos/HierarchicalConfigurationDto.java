package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.tocairn.model.HierarchicalConfiguration;
import me.soels.tocairn.solver.metric.MetricType;

import java.util.List;
import java.util.stream.Collectors;


@Getter
public class HierarchicalConfigurationDto extends SolverConfigurationDto {
    private final List<Double> weights;
    private final Boolean optimizationOnSharedEdges;

    public HierarchicalConfigurationDto(HierarchicalConfiguration dao) {
        super(dao);
        this.weights = dao.getWeights();
        this.optimizationOnSharedEdges = dao.getOptimizationOnSharedEdges();
    }

    @JsonCreator
    public HierarchicalConfigurationDto(List<MetricType> metrics, Integer minClusters, Integer maxClusters, List<Double> weights, Boolean optimizationOnSharedEdges) {
        super(metrics, minClusters, maxClusters);
        this.weights = weights;
        this.optimizationOnSharedEdges = optimizationOnSharedEdges == null || optimizationOnSharedEdges;

        var underlyingMetrics = metrics.stream()
                .flatMap(metricType -> metricType.getMetrics().stream())
                .map(metric -> metric.getClass().getSimpleName())
                .collect(Collectors.toList());
        if (weights.size() != underlyingMetrics.size()) {
            throw new IllegalArgumentException("We need " + underlyingMetrics.size() + " weights. That is one for every metric used. " +
                    "This is for metrics [" + String.join(",", underlyingMetrics) + "]");
        }
    }

    @Override
    public HierarchicalConfiguration toDao() {
        var dao = new HierarchicalConfiguration();
        dao.setMetrics(getMetrics());
        dao.setMinClusterAmount(getMinClusterAmount().orElse(null));
        dao.setMaxClusterAmount(getMaxClusterAmount().orElse(null));
        dao.setWeights(weights);
        return dao;
    }
}
