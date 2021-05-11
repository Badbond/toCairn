package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisModel;

/**
 * Coupling in module (CIM) as suggested by Lindvall et al. (2003). We map the concept of modules to microservices.
 * <p>
 * This metric is calculated per microservice identified and is the average of the coupling between classes (CBC) of a
 * microservice. CBC is measured by counting the amount of dependencies to other classes in the same microservice.
 * <p>
 * See related work 'Lindvall, M., Tvedt, R. T., & Costa, P. (2003). An empirically-based process for software
 * architecture evaluation. <i>Empirical Software Engineering, 8</i>(1), 83-108.'.
 */
public class CouplingInModuleObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, AnalysisModel analysisModel) {
        return -1 * 0.0;
    }
}
