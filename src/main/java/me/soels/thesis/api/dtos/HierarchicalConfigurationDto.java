package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.thesis.model.HierarchicalConfiguration;
import me.soels.thesis.solver.metric.MetricType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Getter
public class HierarchicalConfigurationDto extends SolverConfigurationDto {
    private final List<Double> weights;
    private final Integer nrClusters;

    public HierarchicalConfigurationDto(HierarchicalConfiguration dao) {
        super(dao.getMetrics());
        this.weights = dao.getWeights();
        this.nrClusters = dao.getNrClusters().orElse(null);
    }

    @JsonCreator
    public HierarchicalConfigurationDto(List<MetricType> metrics, List<Double> weights, Integer nrClusters) {
        super(metrics);
        this.weights = weights;
        this.nrClusters = nrClusters;

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
        dao.setNrClusters(nrClusters);
        return dao;
    }

    public Optional<Integer> getNrClusters() {
        return Optional.ofNullable(nrClusters);
    }
}
