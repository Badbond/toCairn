package me.soels.thesis.clustering.objectives;

import me.soels.thesis.clustering.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.AbstractClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Cohesion as measured by Carvalho et al. (2020) based off of metrics in the work of Chidamber and Kemerer (1994).
 * <p>
 * This metric should be maximized but, as the framework only allows for minimization objectives, we negate the value.
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
                // Construct a pair of the cluster's size and the cohesion value for the cluster
                .map(cluster -> Pair.of(cluster.size(), calculateForCluster(cluster)))
                // Apply Carvalho's formula
                // TODO: If clusterSize == 1, this returns Infinity (division by 0).
                //  would 'setting to 0' be good? We want to prevent clusters without inner connections (be it multiple
                //  classes or only 1), I guess. Did I interpret the formula correctly?
                .mapToDouble(pair -> pair.getValue() / (pair.getKey() * (pair.getKey() - 1)) / 2)
                .sum();
    }

    private double calculateForCluster(List<? extends AbstractClass> classes) {
        // We do not use the frequency of the dependency here as Carvalho et al. also made this binary.
        return classes.stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream()
                        .map(relationShip -> Pair.of(clazz, relationShip)))
                .filter(pair -> classes.contains(pair.getValue().getCallee()))
                .count();
    }
}
