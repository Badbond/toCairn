package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisInput;

import java.util.HashMap;

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
    public double calculate(Clustering clustering, AnalysisInput analysisInput) {
        // TODO: Using mock data we did not consider the direction of the relationship which is important for this metric.
        var clusteringByClass = clustering.getByClass();
        var clusterCohesionSums = new HashMap<Integer, Integer>();
        for (var edge : analysisInput.getDependencies()) {
            var cluster = clusteringByClass.get(edge.getCaller());
            if (cluster.equals(clusteringByClass.get(edge.getCallee()))) {
                // TODO: Carvalho does not use frequency within method body but extrapolated towards classes, we do
                //  need to use the amount of methods connecting to other methods to replicate the metric fully.
                clusterCohesionSums.compute(cluster, (key, value) -> value == null ? 1 : value + 1);
            }
        }
        var result = 0.0;
        for (var entry : clusterCohesionSums.entrySet()) {
            double clusterSize = clustering.getByCluster().get(entry.getKey()).size();
            if (clusterSize <= 1) {
                // TODO: If clusterSize == 1, this returns Infinity (division by 0).
                //  would 'setting to 0' be good? We want to prevent clusters without inner connections (be it multiple
                //  classes or only 1), I guess.
                continue;
            }
            // TODO: Did I interpret this function correctly?
            result += entry.getValue() / (clusterSize * (clusterSize - 1)) / 2;
        }

        return -1 * result;
    }
}
