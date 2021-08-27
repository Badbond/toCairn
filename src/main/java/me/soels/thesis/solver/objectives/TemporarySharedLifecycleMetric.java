package me.soels.thesis.solver.objectives;

import me.soels.thesis.solver.moea.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;

public class TemporarySharedLifecycleMetric implements DevelopmentLifecycleMetric {
    // TODO: Calculate shared lifecycle based on evolutionary input
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
