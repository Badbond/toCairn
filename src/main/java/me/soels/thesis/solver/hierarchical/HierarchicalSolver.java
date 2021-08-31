package me.soels.thesis.solver.hierarchical;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.model.*;
import me.soels.thesis.solver.Clustering;
import me.soels.thesis.solver.ClusteringBuilder;
import me.soels.thesis.solver.Solver;
import me.soels.thesis.solver.metric.MetricType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Solver implementation using the agglomorative hierarchical clustering algorithm.
 */
public class HierarchicalSolver implements Solver {
    private final HierarchicalConfiguration configuration;

    public HierarchicalSolver(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public HierarchicalEvaluationResult run(EvaluationInput input) {
        var initialClustering = createInitialSolution(input);
        var allSolutions = new ArrayList<Solution>();
        allSolutions.add(initialClustering.getSolution());

        var singleSolution = performAlgorithm(initialClustering, allSolutions, input);
        var solutions = singleSolution
                .map(Collections::singletonList)
                .orElse(allSolutions);

        var result = new HierarchicalEvaluationResult();
        result.setSolutions(solutions);
        return result;
    }

    /**
     * Performs the agglomorative hierarchical clustering algorithm given an initial clustering.
     * <p>
     * This returns either the preferred solution based on the number of clusters or all solutions from all
     * the intermediate solutions produced by the hierarchical clustering algorithm. The latter includes the
     * clustering with all classes in their own cluster but not the monolithic application itself (all classes in
     * one cluster).
     *
     * @param initialClustering the initial clustering
     * @param allSolutions      a data pass by reference for accumulating all intermittent solutions from the algorithm
     * @param input             the input graph for metrics calculation
     * @return the preferable solution (if present)
     */
    private Optional<Solution> performAlgorithm(HierarchicalClustering initialClustering, ArrayList<Solution> allSolutions, EvaluationInput input) {
        var currentClustering = initialClustering;
        while (true) {
            allSolutions.add(currentClustering.solution);

            if (configuration.getNrClusters().isPresent() &&
                    configuration.getNrClusters().get() == currentClustering.solution.getMicroservices().size()) {
                // Our last iteration was one where the amount of clusters equals that configured. Return it as the single
                // solution.
                return Optional.of(currentClustering.solution);
            } else if (currentClustering.solution.getMicroservices().size() == 1) {
                // We reached the end of the algorithm, thus we did not have a preferable solution
                return Optional.empty();
            }

            var possibleClusterings = getPossibleMergers(currentClustering.clustering);
            currentClustering = getBestMerger(possibleClusterings, input);
        }
    }

    /**
     * Retrieves the best clustering from the given possible clusterings based on the weighed quality function from
     * the configured metrics.
     *
     * @param possibleClusterings the possible clusterings
     * @param input               input data needed for metric calculation
     * @return the best clustering
     */
    private HierarchicalClustering getBestMerger(List<Clustering> possibleClusterings, EvaluationInput input) {
        Double bestQuality = null;
        HierarchicalClustering bestClustering = null;

        // TODO: Parallelize
        for (var clustering : possibleClusterings) {
            var metrics = performMetrics(clustering, input);
            var metricsArray = metrics.values().stream()
                    .flatMapToDouble(Arrays::stream)
                    .toArray();
            var quality = 0.0;
            for (int i = 0; i < configuration.getWeights().size(); i++) {
                var weight = configuration.getWeights().get(i);
                var metric = metricsArray[i];
                quality += metric * weight;
            }
            quality = 1 / (double) metricsArray.length * quality;

            if (bestQuality == null || quality > bestQuality) {
                var solution = new Solution();
                solution.setMetricValues(metrics);
                solution.setMicroservices(clustering.getByCluster().entrySet().stream()
                        .map(cluster -> new Microservice(cluster.getKey(), cluster.getValue()))
                        .collect(Collectors.toList()));
                bestClustering = new HierarchicalClustering(clustering, solution);
                bestQuality = quality;
            }
        }

        if (bestClustering == null) {
            // This should not happen as we always have at least one clustering which we set it to
            throw new IllegalStateException("Could not deduce the best clustering");
        }

        return bestClustering;
    }

    /**
     * Computes the list of possible clusterings from the given clustering.
     * <p>
     * Essentially, we will combine every cluster. We do this using the cluster numbers where we match ever lesser
     * number against every greater number. This ensures all clusters are paired, and only once. This furthermore
     * prevents merging with oneself.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     */
    private List<Clustering> getPossibleMergers(Clustering currentClustering) {
        return currentClustering.getByCluster().keySet().stream()
                .flatMap(clusterA -> currentClustering.getByCluster().keySet().stream()
                        // Only cluster A with clusters with a higher number (excluding duplicate pairs and self-ref.)
                        .filter(clusterB -> clusterA < clusterB)
                        // Create new clustering and merge the two clusters
                        .map(clusterB -> {
                            var builder = new ClusteringBuilder(currentClustering);
                            builder.mergeCluster(clusterA, clusterB);
                            return builder.build();
                        }))
                .collect(Collectors.toList());
    }

    /**
     * Performs the metrics for the given {@link Clustering}.
     *
     * @param clustering the clustering to perform the metrics for
     * @param input      input giving additional information to the metrics
     * @return a data structure containing the resulting metric values
     */
    private Map<MetricType, double[]> performMetrics(Clustering clustering, EvaluationInput input) {
        Map<MetricType, double[]> result = new EnumMap<>(MetricType.class);
        for (var metricType : configuration.getMetrics()) {
            var metricValues = new double[metricType.getMetrics().size()];
            for (int i = 0; i < metricType.getMetrics().size(); i++) {
                metricValues[i] = metricType.getMetrics().get(i).calculate(clustering, input);
            }
            result.put(metricType, metricValues);
        }
        return result;
    }

    /**
     * Creates the initial solution for the agglomorative hierarchical clustering algorithm where every class
     * is placed in its own cluster.
     * <p>
     * We also calculate the metrics for this solution such that it can be used in analysis when the user has not
     * specified the number of desired clusters.
     *
     * @param input the input to cluster on.
     * @return the initial clustering solution
     */
    private HierarchicalClustering createInitialSolution(EvaluationInput input) {
        var builder = new ClusteringBuilder();
        for (int i = 0; i < input.getOtherClasses().size(); i++) {
            builder.addToCluster(input.getOtherClasses().get(i), i);
        }
        var clustering = builder.build();

        var solution = new Solution();
        solution.setMetricValues(performMetrics(clustering, input));
        solution.setMicroservices(clustering.getByCluster().entrySet().stream()
                .map(entry -> new Microservice(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));

        return new HierarchicalClustering(clustering, solution);
    }

    @Getter
    @AllArgsConstructor
    private static class HierarchicalClustering {
        private final Clustering clustering;
        private final Solution solution;
    }
}
