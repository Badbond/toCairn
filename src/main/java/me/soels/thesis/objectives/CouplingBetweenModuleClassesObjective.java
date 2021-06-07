package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public double calculate(Clustering clustering, AnalysisInput analysisInput) {
        Map<Integer, List<Integer>> interClusterDependencies = clustering.getByCluster().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>()));
        var clusteringByClass = clustering.getByClass();
        for (var edge : analysisInput.getDependencies()) {
            var classA = edge.getCaller();
            var classB = edge.getCallee();

            if (!clusteringByClass.get(classA).equals(clusteringByClass.get(classB))) {
                interClusterDependencies.get(clusteringByClass.get(classB)).add(clusteringByClass.get(classA));
                interClusterDependencies.get(clusteringByClass.get(classA)).add(clusteringByClass.get(classB));
            }
        }

        return clustering.getByCluster().keySet().stream()
                .mapToDouble(otherClasses -> interClusterDependencies.get(otherClasses).size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBMC for this solution"));
    }
}
