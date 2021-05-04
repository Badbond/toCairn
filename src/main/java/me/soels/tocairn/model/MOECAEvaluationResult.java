package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.metric.MetricType;
import me.soels.tocairn.solver.moeca.MOECASolver;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Models the result from a {@link MOECASolver}.
 */
@Node
@Getter
@Setter
public class MOECAEvaluationResult extends EvaluationResult {
    @CompositeProperty(prefix = "populationMetrics")
    private Map<String, Double> populationMetrics = new HashMap<>();

    @CompositeProperty(prefix = "minMetricValues")
    private Map<MetricType, double[]> minMetricValues = new EnumMap<>(MetricType.class);

    @CompositeProperty(prefix = "maxMetricValues")
    private Map<MetricType, double[]> maxMetricValues = new EnumMap<>(MetricType.class);
}
