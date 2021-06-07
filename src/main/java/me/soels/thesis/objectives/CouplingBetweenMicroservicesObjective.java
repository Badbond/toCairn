package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public double calculate(Clustering clustering, AnalysisInput analysisInput) {
        Map<Integer, List<Integer>> interClusterDependencies = clustering.getByCluster().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>()));
        var clusteringByClass = clustering.getByClass();
        for (var edge : analysisInput.getDependencies()) {
            var classA = edge.getCaller();
            var classB = edge.getCallee();

            if (!clusteringByClass.get(classA).equals(clusteringByClass.get(classB))) {
                addDependency(interClusterDependencies, clusteringByClass.get(classA), clusteringByClass.get(classB));
                addDependency(interClusterDependencies, clusteringByClass.get(classB), clusteringByClass.get(classA));
            }
        }

        return clustering.getByCluster().entrySet().stream()
                .mapToDouble(entry -> interClusterDependencies.get(entry.getKey()).size() / (double) entry.getValue().size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBM for this solution"));
    }

    private void addDependency(Map<Integer, List<Integer>> interClusterDependencies,
                               Integer clusterA,
                               Integer clusterB) {
        if (!interClusterDependencies.get(clusterB).contains(clusterA)) {
            interClusterDependencies.get(clusterB).add(clusterA);
        }
    }
}
