package me.soels.tocairn.solver.metric;

import me.soels.tocairn.solver.Clustering;

/**
 * Modularity metric as devised by Clauset-Newman-Moore (2004) based on dynamic runtime coupling between classes as
 * devised by Löhnzertz (2020).
 * <p>
 * Löhnzertz combines the coupling weights during construction of the graph using a weighed function. Then they use
 * a hierarchical clustering algorithm for optimizing the modularity. We on the other hand want to optimize for these
 * coupling criteria. Therefore, we construct this modularity metric for every coupling criteria separately.
 * <p>
 * See related work 'J. Löhnertz, “Toward automatic decomposition of monolithic software into microservices,”
 * Last accessed: 2021-04-16, M.S. thesis, University of Amsterdam, 2020. doi: 10.5281/zenodo.4280725. [Online].
 * Available: https://zenodo.org/record/4280725.'.
 */
public class LohnertzDynamicCoupling implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        // TODO: Implement
        return 0;
    }
}
