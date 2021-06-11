package me.soels.thesis.clustering.objectives;

import me.soels.thesis.clustering.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;

public class TemporaryBoundedContextMetric implements DataAutonomyMetric {
    // TODO: Calculate bounded context based on static input (class names)
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
