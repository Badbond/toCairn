package me.soels.tocairn.solver.metric;

import me.soels.tocairn.solver.Clustering;


/**
 * Modularity metric as devised by Clauset-Newman-Moore (2004) based on evolutionary coupling between classes as
 * devised by Löhnzertz (2020).
 * <p>
 * See related work 'J. Löhnertz, “Toward automatic decomposition of monolithic software into microservices,”
 * Last accessed: 2021-04-16, M.S. thesis, University of Amsterdam, 2020. doi: 10.5281/zenodo.4280725. [Online].
 * Available: https://zenodo.org/record/4280725.'.
 */
public class LohnertzEvolutionaryCouplingModularity implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        // TODO: Implement
        return 0;
    }
}
