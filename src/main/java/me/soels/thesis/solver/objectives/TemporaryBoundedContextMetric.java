package me.soels.thesis.solver.objectives;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.solver.Clustering;

public class TemporaryBoundedContextMetric implements OnePurposeMetric {
    // TODO: Calculate bounded context based on static input (class names)
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
