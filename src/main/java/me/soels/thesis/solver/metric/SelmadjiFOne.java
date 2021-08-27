package me.soels.thesis.solver.metric;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.solver.Clustering;

import java.util.List;

public class SelmadjiFOne extends SelmadjiStructuralBehavior {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        // TODO: Calculate FOne
        return 0.0;
    }
    private double interCoup(List<OtherClass> classes) {
        return 0.0;
    }

    private double interCoh(List<OtherClass> classes) {
        return 0.0;
    }
}
