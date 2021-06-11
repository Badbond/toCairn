package me.soels.thesis.clustering.objectives;

import me.soels.thesis.clustering.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;

public class TemporaryDataAutonomyMetric implements DataAutonomyMetric {
    // TODO: Calculate data autonomy based on static and dynamic input
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
