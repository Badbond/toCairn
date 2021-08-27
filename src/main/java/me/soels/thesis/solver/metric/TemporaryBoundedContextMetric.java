package me.soels.thesis.solver.metric;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.solver.Clustering;

public class TemporaryBoundedContextMetric implements Metric {
    // TODO: Calculate bounded context based on static input (class names)
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
