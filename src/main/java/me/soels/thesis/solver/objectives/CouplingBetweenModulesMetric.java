package me.soels.thesis.solver.objectives;

import me.soels.thesis.solver.Clustering;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;

import java.util.*;

/**
 * Coupling between modules (CBM) as suggested by Lindvall et al. (2003). We map the concept of modules to
 * microservices.
 * <p>
 * The metric is calculated per microservice identified. We calculate it as an average over the whole solution as Taibi,
 * D. and Systä, K. used that to compare solutions for their similar metric coupling between microservices (CBMs) as
 * well. The difference in metrics is CBMs is a ratio to the amount of classes in the module/microservice and.
 * <p>
 * See related work 'Lindvall, M., Tvedt, R. T., & Costa, P. (2003). An empirically-based process for software
 * architecture evaluation. <i>Empirical Software Engineering, 8</i>(1), 83-108.'.
 */
public class CouplingBetweenModulesMetric implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        // Construct a set of which cluster depends on which other cluster in both directions.
        // Note, we use a set as CBM does not distinguish different classes within the module.
        var clusterDependencies = new HashMap<Integer, Set<Integer>>();
        clustering.getByCluster().keySet().forEach(clusterNumber -> clusterDependencies.put(clusterNumber, new HashSet<>()));
        clustering.getByCluster().forEach((clusterFrom, classes) -> classes
                .forEach(clazz -> populateDependenciesForClass(clazz, classes, clusterFrom, clustering.getByClass(), clusterDependencies)));


        return clustering.getByCluster().keySet().stream()
                .mapToDouble(clusterNumber -> clusterDependencies.get(clusterNumber).size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBM for this solution"));
    }

    private void populateDependenciesForClass(OtherClass clazz,
                                              List<? extends AbstractClass> ownClusterClasses,
                                              Integer ownCluster,
                                              Map<? extends AbstractClass, Integer> byClass,
                                              HashMap<Integer, Set<Integer>> clusterDependencies) {
        clazz.getDependenceRelationships().stream()
                .filter(relationship -> !ownClusterClasses.contains(relationship.getCallee()))
                .forEach(relationship -> {
                    var targetCluster = byClass.get(relationship.getCallee());
                    // Register cluster dependency in both clusters as the metric considers both incoming
                    // and outgoing dependencies
                    clusterDependencies.get(ownCluster).add(targetCluster);
                    clusterDependencies.get(targetCluster).add(ownCluster);
                });
    }
}
