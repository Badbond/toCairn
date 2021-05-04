package me.soels.tocairn.api.dtos;

import lombok.Getter;
import me.soels.tocairn.model.MOECAEvaluationResult;
import me.soels.tocairn.solver.metric.MetricType;

import java.util.Map;

@Getter
public class MOECAEvaluationResultDto extends EvaluationResultDto {
    private final Map<String, Double> populationMetrics;
    private final Map<MetricType, double[]> minMetricValues;
    private final Map<MetricType, double[]> maxMetricValues;

    public MOECAEvaluationResultDto(MOECAEvaluationResult result) {
        super(result);
        this.populationMetrics = result.getPopulationMetrics();
        this.minMetricValues = result.getMinMetricValues();
        this.maxMetricValues = result.getMaxMetricValues();
    }
}
