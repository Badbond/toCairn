package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.tmp.daos.AbstractClass;

import java.util.List;
import java.util.Map;

/**
 * Coupling between microservices (CBMs) as suggested by Taibi and Systä (2019) based off of Coupling Between Object
 * (CBO) metric of Chidamber and Kemerer (1994).
 * <p>
 * The metric is calculated per microservice identified. We calculate it as an average over the whole solution as Taibi
 * and Systä compared solutions that way as well.
 * <p>
 * See related work 'Taibi, D., & Systä, K.(2019, May). From Monolithic Systems to Microservices: A Decomposition
 * Framework based on Process Mining. In <i>CLOSER</i> (pp. 153-164).' and 'Chidamber, S. R., & Kemerer, C. F. (1994).
 * A metrics suite for object oriented design. <i>IEEE Transactions on software engineering, 20</i>(6), 476-493.'.
 */
public class CouplingBetweenMicroservicesObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return clustering.getByCluster().values().stream()
                .mapToDouble(cluster -> getUniqueExternalLinks(clustering.getByClass(), cluster) / cluster.size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBM for this solution"));
    }

    private double getUniqueExternalLinks(Map<? extends AbstractClass, Integer> clusteringByClass, List<? extends AbstractClass> cluster) {
        return cluster.stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                .filter(relation -> !cluster.contains(relation.getCallee()))
                .mapToInt(relation -> clusteringByClass.get(relation.getCallee()))
                .distinct()
                .sum();
    }
}
