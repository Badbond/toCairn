package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.tmp.daos.AbstractClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Coupling between module classes (CBMC) as suggested by Lindvall et al. (2003). We map the concept of modules to
 * microservices.
 * <p>
 * The metric is calculated per microservice identified. We calculate it as an average over the whole solution as Taibi,
 * D. and Systä, K. used that to compare solutions for their similar metric coupling between microservices (CBMs) as
 * well. The difference in metrics is (1) CBMs is a ratio to the amount of classes in the module/microservice and (2)
 * CBMs counts multiple dependencies to the same microservice as one (similar to CBM).
 * <p>
 * See related work 'Lindvall, M., Tvedt, R. T., & Costa, P. (2003). An empirically-based process for software
 * architecture evaluation. <i>Empirical Software Engineering, 8</i>(1), 83-108.'.
 */
public class CouplingBetweenModuleClassesObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        var clusterIn = new HashMap<Integer, Set<Integer>>();
        clustering.getByCluster().keySet().forEach(clusterNumber -> clusterIn.put(clusterNumber, new HashSet<>()));
        var clusterOut = new HashMap<Integer, Set<Integer>>();
        clustering.getByCluster().keySet().forEach(clusterNumber -> clusterOut.put(clusterNumber, new HashSet<>()));

        // Create a map of incoming dependencies of other cluster for each cluster
        clustering.getByCluster().forEach((clusterNumber, cluster) ->
                populateIncomingDependencies(clustering.getByClass(), clusterIn, clusterNumber, cluster));

        populateOutgoingDependencies(clusterIn, clusterOut);

        return clustering.getByCluster().keySet().stream()
                .mapToDouble(clusterNumber -> clusterIn.get(clusterNumber).size() + clusterOut.get(clusterNumber).size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBMC for this solution"));
    }

    private void populateIncomingDependencies(Map<? extends AbstractClass, Integer> byClass,
                                              HashMap<Integer, Set<Integer>> clusterIn,
                                              Integer clusterNumber,
                                              List<? extends AbstractClass> cluster) {
        cluster.stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                .filter(relationship -> !cluster.contains(relationship.getCallee()))
                .forEach(relationship -> clusterIn.get(byClass.get(relationship.getCallee())).add(clusterNumber));
    }

    private void populateOutgoingDependencies(HashMap<Integer, Set<Integer>> clusterIn,
                                              HashMap<Integer, Set<Integer>> clusterOut) {
        clusterIn.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(incoming -> Pair.of(incoming, entry.getKey())))
                .forEach(pair -> clusterOut.get(pair.getKey()).add(pair.getValue()));
    }
}
