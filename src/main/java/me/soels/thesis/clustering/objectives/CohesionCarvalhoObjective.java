package me.soels.thesis.clustering.objectives;

import me.soels.thesis.clustering.encoding.Clustering;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.EvaluationInput;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Cohesion as measured by Carvalho et al. (2020) based off of metrics in the work of Chidamber and Kemerer (1994).
 * <p>
 * This metric should be maximized but, as the framework only allows for minimization objectives, we negate the value.
 * <p>
 * The only addition we have made is to give clusters of 1 node a value of 0.25 as they, like a cluster with 2 nodes
 * and a joining edge, the most cohesive form. This counters the 'cluster everything together' effect of coupling
 * metrics. Otherwise, we would have a division by 0 returning in NaN.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE Transactions on software engineering, 20</i>(6), 476-493.'.
 */
public class CohesionCarvalhoObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        var clusteringByCluster = clustering.getByCluster();

        return -1 * clusteringByCluster.values().stream()
                // Construct a pair of the cluster's size and the amount of connections within the cluster
                .map(cluster -> Pair.of((double) cluster.size(), countInnerRelations(cluster)))
                // Apply Carvalho's formula to calculate the cohesion of the cluster
                .mapToDouble(this::calculateForCluster)
                .sum();
    }

    private double calculateForCluster(Pair<Double, Double> pair) {
        if (pair.getKey() <= 1.0) {
            // There is only one node in the cluster. This is an 'optimal clustering' in terms of cohesion.
            return 0.25;
        }
        return pair.getValue() / (pair.getKey() * (pair.getKey() - 1)) / 2;
    }

    private double countInnerRelations(List<? extends AbstractClass> classes) {
        // We do not use the frequency of the dependency here as Carvalho et al. also made this binary.
        return classes.stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream()
                        .map(relationShip -> Pair.of(clazz, relationShip)))
                .filter(pair -> classes.contains(pair.getValue().getCallee()))
                .count();
    }
}
