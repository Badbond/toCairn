package me.soels.thesis.solver.metric;

import me.soels.thesis.model.OtherClass;

/**
 * Metric for Functional interface to identify a metric for the {@link MetricType#DATA_AUTONOMY} objective.
 * <p>
 * Based on the metric and its definition as proposed by Selmadji et al. (2020). This class also contains metrics shared
 * between both {@link SelmadjiFInter} and {@link SelmadjiFIntra}.
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 *
 * @see SelmadjiFAutonomy
 * @see SelmadjiFOne
 */
public abstract class SelmadjiStructuralBehavior implements Metric {
    protected double coup(OtherClass i, OtherClass j) {
        return 0.0;
    }
}
