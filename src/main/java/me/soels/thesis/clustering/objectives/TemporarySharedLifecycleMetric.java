package me.soels.thesis.clustering.objectives;

import me.soels.thesis.clustering.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;

public class TemporarySharedLifecycleMetric implements DataAutonomyMetric {
    // TODO: Calculate shared lifecycle based on evolutionary input
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
