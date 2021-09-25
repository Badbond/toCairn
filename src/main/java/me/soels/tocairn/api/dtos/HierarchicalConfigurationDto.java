package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.tocairn.model.HierarchicalConfiguration;
import me.soels.tocairn.solver.metric.MetricType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Getter
public class HierarchicalConfigurationDto extends SolverConfigurationDto {
    private final List<Double> weights;
    private final Integer minClusters;
    private final Integer maxClusters;

    public HierarchicalConfigurationDto(HierarchicalConfiguration dao) {
        super(dao.getMetrics());
        this.weights = dao.getWeights();
        this.minClusters = dao.getMinClusters().orElse(null);
        this.maxClusters = dao.getMaxClusters().orElse(null);
    }

    @JsonCreator
    public HierarchicalConfigurationDto(List<MetricType> metrics, List<Double> weights, Integer minClusters, Integer maxClusters) {
        super(metrics);
        this.weights = weights;
        this.minClusters = minClusters;
        this.maxClusters = maxClusters;

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
        dao.setWeights(weights);
        dao.setMinClusters(minClusters);
        dao.setMaxClusters(maxClusters);
        return dao;
    }

    public Optional<Integer> getMinClusters() {
        return Optional.ofNullable(minClusters);
    }

    public Optional<Integer> getMaxClusters() {
        return Optional.ofNullable(maxClusters);
    }
}
