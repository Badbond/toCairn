package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.tocairn.model.AHCAConfiguration;
import me.soels.tocairn.solver.metric.MetricType;

import java.util.List;
import java.util.stream.Collectors;


@Getter
public class AHCAConfigurationDto extends SolverConfigurationDto {
    private final List<Double> weights;
    private final boolean optimisationOnSharedEdges;
    private final boolean normaliseMetrics;

    public AHCAConfigurationDto(AHCAConfiguration dao) {
        super(dao);
        this.weights = dao.getWeights();
        this.optimisationOnSharedEdges = dao.isOptimizationOnSharedEdges();
        this.normaliseMetrics = dao.isNormalizeMetrics();
    }

    @JsonCreator
    public AHCAConfigurationDto(List<MetricType> metrics, Integer minClusters, Integer maxClusters,
                                List<Double> weights, Boolean optimisationOnSharedEdges, Boolean normaliseMetrics) {
        super(metrics, minClusters, maxClusters);
        this.weights = weights;
        this.optimisationOnSharedEdges = optimisationOnSharedEdges == null || optimisationOnSharedEdges;
        this.normaliseMetrics = normaliseMetrics == null || normaliseMetrics;

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
    public AHCAConfiguration toDao() {
        var dao = new AHCAConfiguration();
        dao.setMetrics(getMetrics());
        dao.setMinClusterAmount(getMinClusterAmount().orElse(null));
        dao.setMaxClusterAmount(getMaxClusterAmount().orElse(null));
        dao.setWeights(weights);
        dao.setOptimizationOnSharedEdges(optimisationOnSharedEdges);
        dao.setNormalizeMetrics(normaliseMetrics);
        return dao;
    }
}
