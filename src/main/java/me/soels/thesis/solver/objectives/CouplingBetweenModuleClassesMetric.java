package me.soels.thesis.solver.objectives;

import me.soels.thesis.solver.Clustering;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.EvaluationInput;

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
public class CouplingBetweenModuleClassesMetric implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        // Construct a set of which cluster depends on which other cluster in both directions.
        // Note, we use a set of classes as CBMC does (unlike CBM) distinguish different classes within the modules.
        var clusterDependencies = new HashMap<Integer, Set<AbstractClass>>();
        clustering.getByCluster().keySet().forEach(clusterNumber -> clusterDependencies.put(clusterNumber, new HashSet<>()));
        clustering.getByCluster().forEach((clusterFrom, classes) -> classes
                .forEach(clazz -> populateDependenciesForClass(clazz, classes, clusterFrom, clustering.getByClass(), clusterDependencies)));

        return clustering.getByCluster().keySet().stream()
                .mapToDouble(clusterNumber -> clusterDependencies.get(clusterNumber).size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBMC for this solution"));
    }

    private void populateDependenciesForClass(AbstractClass clazz,
                                              List<? extends AbstractClass> classes,
                                              Integer clusterFrom,
                                              Map<? extends AbstractClass, Integer> byClass,
                                              HashMap<Integer, Set<AbstractClass>> clusterDependencies) {
        clazz.getDependenceRelationships().stream()
                .filter(relationship -> !classes.contains(relationship.getCallee()))
                .forEach(relationship -> {
                    var clusterTarget = byClass.get(relationship.getCallee());
                    // Add target class to cluster of the source class as dependency
                    clusterDependencies.get(clusterFrom).add(relationship.getCallee());
                    // Add source class to cluster of the target class as dependency
                    clusterDependencies.get(clusterTarget).add(clazz);
                });
    }
}
