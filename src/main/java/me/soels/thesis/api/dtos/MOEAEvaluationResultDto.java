package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.MOEAEvaluationResult;

import java.util.Map;

@Getter
public class MOEAEvaluationResultDto extends EvaluationResultDto {
    private final Map<String, Double> populationMetrics;

    public MOEAEvaluationResultDto(MOEAEvaluationResult result) {
        super(result);
        this.populationMetrics = result.getPopulationMetrics();
    }
}
