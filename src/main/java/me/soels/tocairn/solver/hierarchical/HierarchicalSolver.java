package me.soels.tocairn.solver.hierarchical;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.tocairn.model.*;
import me.soels.tocairn.solver.Clustering;
import me.soels.tocairn.solver.ClusteringBuilder;
import me.soels.tocairn.solver.Solver;
import me.soels.tocairn.solver.metric.MetricType;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Solver implementation using the agglomorative hierarchical clustering algorithm.
 */
public class HierarchicalSolver implements Solver {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalSolver.class);
    private final HierarchicalConfiguration configuration;

    public HierarchicalSolver(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public HierarchicalEvaluationResult run(EvaluationInput input) {
        LOGGER.info("Running hierarchical solver");
        var initialClustering = createInitialSolution(input);
        var allSolutions = new ArrayList<Solution>();

        var singleSolution = performAlgorithm(initialClustering, allSolutions);
        var solutions = singleSolution
                .map(Collections::singletonList)
                .orElse(allSolutions);

        var result = new HierarchicalEvaluationResult();
        result.setSolutions(solutions);
        LOGGER.info("Hierarchical solver produced {} solutions", solutions.size());
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
     * @return the preferable solution (if present)
     */
    private Optional<Solution> performAlgorithm(HierarchicalClustering initialClustering, ArrayList<Solution> allSolutions) {
        var currentClustering = initialClustering;
        int counter = 0;
        while (true) {
            var start = System.currentTimeMillis();
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

            var possibleClusterings = getPossibleMergersWithSharedEdge(currentClustering.clustering);
            var best = getBestMerger(possibleClusterings);
            if (best.isEmpty()) {
                return Optional.empty();
            }
            currentClustering = best.get();
            var duration = DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "mm:ss.SSS");
            LOGGER.info("Performed step {} in the clustering algorithm producing {} microservices in {} (m:s.millis)",
                    ++counter, currentClustering.getClustering().getByCluster().size(), duration);
        }
    }

    /**
     * Retrieves the best clustering from the given possible clusterings based on the weighed quality function from
     * the configured metrics.
     * <p>
     * Returns empty when we could not give the best clustering. In that case, we should safely stop the algorithm
     * and persist intermittent results for investigation.
     *
     * @param possibleClusterings the possible clusterings
     * @return the best clustering
     */
    private Optional<HierarchicalClustering> getBestMerger(List<Clustering> possibleClusterings) {
        AtomicReference<Double> bestQuality = new AtomicReference<>();
        AtomicReference<HierarchicalClustering> bestClustering = new AtomicReference<>();

        possibleClusterings.parallelStream()
                .forEach(clustering -> processClusteringParallel(clustering, bestQuality, bestClustering));

        if (bestClustering.get() == null) {
            // Apparently, no best clustering is given, return null to stop the algorithm
            return Optional.empty();
        }

        return Optional.ofNullable(bestClustering.get());
    }

    private void processClusteringParallel(Clustering clustering,
                                           AtomicReference<Double> bestQuality,
                                           AtomicReference<HierarchicalClustering> bestClustering) {
        var metrics = performMetrics(clustering);
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
        checkAndSetBestQuality(clustering, bestClustering, bestQuality, metrics, quality);
    }

    private synchronized void checkAndSetBestQuality(Clustering clustering,
                                                     AtomicReference<HierarchicalClustering> bestClustering,
                                                     AtomicReference<Double> bestQuality,
                                                     Map<MetricType, double[]> metrics,
                                                     double quality) {
        // As we negate maximization functions, the best quality is represented by the lowest number. Hence, lesser than.
        if (bestQuality.get() == null || quality < bestQuality.get()) {
            var solution = new Solution();
            solution.setMetricValues(metrics);
            solution.setMicroservices(clustering.getByCluster().entrySet().stream()
                    .map(cluster -> new Microservice(cluster.getKey(), cluster.getValue()))
                    .collect(Collectors.toList()));
            bestClustering.set(new HierarchicalClustering(clustering, solution));
            bestQuality.set(quality);
        }
    }

    /**
     * Computes the list of possible clusterings from the given clustering.
     * <p>
     * Note, similar to Clauset, Newman & Moore, here we only allow merging between microservices that share an edge.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     */
    public List<Clustering> getPossibleMergersWithSharedEdge(Clustering currentClustering) {
        return currentClustering.getByCluster().entrySet().parallelStream()
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                        .map(rel -> currentClustering.getByClass().get(rel.getCallee()))
                        .distinct()
                        .filter(key -> !entry.getKey().equals(key))
                        .map(other -> other < entry.getKey() ? other + ":" + entry.getKey() : entry.getKey() + ":" + other))
                .distinct()
                .map(key -> key.split(":"))
                .map(parts -> Pair.of(Integer.valueOf(parts[0]), Integer.valueOf(parts[1])))
                .map(pair -> {
                    var merger = new ClusteringBuilder(currentClustering);
                    merger.mergeCluster(pair.getKey(), pair.getValue());
                    return merger.build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Computes the list of possible clusterings from the given clustering.
     * <p>
     * Note, similar to Clauset, Newman & Moore, here we only allow merging between microservices that share an edge.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     */
    public List<Clustering> getPossibleMergers(Clustering currentClustering) {
        return currentClustering.getByCluster().keySet().parallelStream()
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
     * @return a data structure containing the resulting metric values
     */
    private Map<MetricType, double[]> performMetrics(Clustering clustering) {
        Map<MetricType, double[]> result = new EnumMap<>(MetricType.class);
        for (var metricType : configuration.getMetrics()) {
            var metricValues = new double[metricType.getMetrics().size()];
            for (int i = 0; i < metricType.getMetrics().size(); i++) {
                metricValues[i] = metricType.getMetrics().get(i).calculate(clustering);
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
        solution.setMetricValues(performMetrics(clustering));
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
