package me.soels.thesis.solver.objectives;

import me.soels.thesis.solver.moea.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;

public class SelmadjiDataAutonomyMetric implements DataAutonomyMetric {
    // TODO: Calculate data autonomy based on static and dynamic input
    // TODO: Make sure that defaulting is configurable such that we can run multiple analysis with different policies.
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return 0;
    }
}
