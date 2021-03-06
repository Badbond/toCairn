package me.soels.tocairn.solver.metric;

import me.soels.tocairn.solver.Clustering;

/**
 * A metric measures how well a clustering performs in terms of (part of) a characteristic of the input data. This
 * defines the dimensions of solving the microservice boundary identification problem to give microservice
 * recommendations relevant to these metrics.
 * <p>
 * For example, {@link MetricType#SHARED_DEVELOPMENT_LIFECYCLE} represents how well the microservices have a shared
 * development lifecycle.
 * <p>
 * Note that the metrics that should be evaluated should be represented as a minimization problem. If it is not,
 * it should be negated. This is required for the MOEA framework used in the respective solver.
 */
public interface Metric {
    /**
     * Calculate the metric value for the given clustering based on the given application input.
     * <p>
     * The clustering only contains the nodes that are combined whereas the application input should hold more
     * information needed to determine the resulting metrics value.
     *
     * @param clustering the decoded clustering as generated by the evolutionary algorithm
     * @return the resulting value for this metric
     */
    double calculate(Clustering clustering);
}
