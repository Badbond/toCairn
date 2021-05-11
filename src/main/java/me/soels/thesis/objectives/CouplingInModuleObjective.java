package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisModel;

/**
 * Coupling in module (CIM) as suggested by Lindvall et al. (2003). We map the concept of modules to microservices.
 * <p>
 * This metric is calculated per microservice identified and is the average of the coupling between classes (CBC) of a
 * microservice. CBC is measured by counting the amount of dependencies to other classes in the same microservice. We
 * then further average this metric to apply it based on the whole solution (similar to CBM, CBMs, CBMC).
 * <p>
 * Furthermore, we negate the values as the framework only allows for minimization objectives.
 * <p>
 * See related work 'Lindvall, M., Tvedt, R. T., & Costa, P. (2003). An empirically-based process for software
 * architecture evaluation. <i>Empirical Software Engineering, 8</i>(1), 83-108.'.
 */
public class CouplingInModuleObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, AnalysisModel analysisModel) {
        var accumulatedCim = 0.0;
        for (var cluster : clustering.getByCluster().values()) {
            var sumOfDependencies = analysisModel.getDependencies().stream()
                    .filter(dependency -> cluster.contains(dependency.getFirst()) && cluster.contains(dependency.getSecond()))
                    .count();
            accumulatedCim += sumOfDependencies / (double) cluster.size();
        }

        return -1 * accumulatedCim / clustering.getByCluster().size();
    }
}
