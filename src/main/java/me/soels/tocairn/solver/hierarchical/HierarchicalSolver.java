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
    private Map<OtherClass, Set<OtherClass>> classConnections;

    public HierarchicalSolver(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public HierarchicalEvaluationResult run(EvaluationInput input) {
        LOGGER.info("Running hierarchical solver");
        classConnections = calculateClassConnectedness(input);
        var initialClustering = createInitialSolution(input);

        var solutions = performAlgorithm(initialClustering);

        var result = new HierarchicalEvaluationResult();
        result.setSolutions(solutions);
        LOGGER.info("Hierarchical solver produced {} solutions", solutions.size());
        return result;
    }

    /**
     * Performs the agglomerative hierarchical clustering algorithm given an initial clustering.
     *
     * @param initialClustering the initial clustering
     * @return the preferable solution (if present)
     */
    private List<Solution> performAlgorithm(HierarchicalClustering initialClustering) {
        var solutionsToPersist = new ArrayList<Solution>();
        var currentClustering = initialClustering;
        int counter = 0;
        int minClusters = configuration.getMinClusterAmount().orElse(1);
        int maxClusters = configuration.getMaxClusterAmount().orElse(initialClustering.clustering.getByClass().size());

        while (true) {
            var start = System.currentTimeMillis();

            var size = currentClustering.solution.getMicroservices().size();
            if (size <= maxClusters) {
                solutionsToPersist.add(currentClustering.solution);
            }
            if (size == minClusters) {
                return solutionsToPersist;
            }

            var possibleMergers = getPossibleMergers(currentClustering);
            var best = getBestMerger(possibleMergers, currentClustering);
            if (best.isEmpty()) {
                LOGGER.info("No best next cluster found in {} possible clusterings. Stopping.", possibleMergers.size());
                return solutionsToPersist;
            }
            currentClustering = best.get();
            var duration = DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "mm:ss.SSS");
            LOGGER.info("Performed step {} in the clustering algorithm producing {} microservices in {} (m:s.millis)",
                    ++counter, currentClustering.getClustering().getByCluster().size(), duration);
        }
    }

    /**
     * Retrieves the best clustering from the given possible mergers for the current clustering based on the weighed
     * quality function from the configured metrics.
     * <p>
     * Returns empty when we could not give the best clustering. In that case, we should safely stop the algorithm
     * and persist intermittent results for investigation.
     *
     * @param possibleMergers   the possible pairs of keys to merge
     * @param currentClustering the previous step's best clustering
     * @return the best clustering
     */
    private Optional<HierarchicalClustering> getBestMerger(Set<Pair<Integer, Integer>> possibleMergers, HierarchicalClustering currentClustering) {
        AtomicReference<Double> bestQuality = new AtomicReference<>();
        AtomicReference<HierarchicalClustering> bestClustering = new AtomicReference<>();

        possibleMergers.parallelStream()
                .map(pair -> {
                    var merger = new ClusteringBuilder(currentClustering.clustering);
                    merger.mergeCluster(pair.getKey(), pair.getValue());
                    return merger.build();
                })
                .forEach(clustering -> processClusteringParallel(clustering, bestQuality, bestClustering));
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
        quality = quality / configuration.getWeights().stream().mapToDouble(v -> v).sum();
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

    private Set<Pair<Integer, Integer>> getPossibleMergers(HierarchicalClustering currentClustering) {
        var sharingAnEdge = getPossibleMergersWithSharedEdge(currentClustering.clustering);
        if (!sharingAnEdge.isEmpty()) {
            return sharingAnEdge;
        } else {
            return getAllPossibleMergers(currentClustering.clustering);
        }
    }

    /**
     * Computes the set of possible clusterings from the given clustering.
     * <p>
     * Returns an empty set if {@link HierarchicalConfiguration#isOptimizationOnSharedEdges()} is set to {@code false}.
     * <p>
     * Note, similar to Clauset, Newman & Moore, here we only allow merging between microservices that share an edge.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     */
    public Set<Pair<Integer, Integer>> getPossibleMergersWithSharedEdge(Clustering currentClustering) {
        if (!configuration.isOptimizationOnSharedEdges()) {
            return Collections.emptySet();
        }

        return currentClustering.getByCluster().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(clazz -> classConnections.get(clazz).stream())
                        .map(connectedClass -> currentClustering.getByClass().get(connectedClass))
                        .filter(ms -> !entry.getKey().equals(ms))
                        .map(ms -> entry.getKey() < ms ? Pair.of(entry.getKey(), ms) : Pair.of(ms, entry.getKey())))
                .collect(Collectors.toSet());
    }

    /**
     * Computes the set of all possible clusterings from the given clustering.
     * <p>
     * This creates a set of pairs of integers indicating which pair of microservices
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     * @see <a href="https://www.baeldung.com/java-combinations-algorithm">Baeldung Java combinations algorithm</a>
     */
    public Set<Pair<Integer, Integer>> getAllPossibleMergers(Clustering currentClustering) {
        List<int[]> combinations = new ArrayList<>();
        helper(combinations, new int[2], 0, currentClustering.getByCluster().size() - 1, 0);

        return combinations.stream()
                .map(combination -> Pair.of(combination[0], combination[1]))
                .collect(Collectors.toSet());
    }

    private void helper(List<int[]> combinations, int[] data, int start, int end, int index) {
        if (index == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else if (start <= end) {
            data[index] = start;
            helper(combinations, data, start + 1, end, index + 1);
            helper(combinations, data, start + 1, end, index);
        }
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

    /**
     * Returns a map representing the connected {@link OtherClass} for one {@link OtherClass}.
     * <p>
     * This not only contains those defined by the class' {@link OtherClass#getDependenceRelationships()}, but also
     * those linked through (recursive) {@link OtherClass#getDataRelationships()}. This recursiveness is based
     * on both incoming and outgoing data dependencies.
     *
     * @param input the input to base this structure upon
     * @return the map representing the connectedness of classes to cluster
     */
    private Map<OtherClass, Set<OtherClass>> calculateClassConnectedness(EvaluationInput input) {
        Map<OtherClass, Set<OtherClass>> result = input.getOtherClasses().stream()
                .collect(Collectors.toMap(dataClass -> dataClass, v -> new HashSet<>()));
        Map<DataClass, Set<OtherClass>> dataConnections = input.getDataClasses().stream()
                .collect(Collectors.toMap(dataClass -> dataClass, v -> new HashSet<>()));
        Map<DataClass, Set<DataClass>> relatedDataClasses = input.getDataClasses().stream()
                .collect(Collectors.toMap(dataClass -> dataClass, v -> new HashSet<>()));

        // Direct relationships
        input.getOtherClasses().forEach(clazz ->
                result.get(clazz).addAll(clazz.getDependenceRelationships().stream()
                        .map(DependenceRelationship::getCallee)
                        .map(OtherClass.class::cast) // Guaranteed by relationship from originating other class
                        .collect(Collectors.toSet()))
        );

        // Add incoming relationships to data classes
        input.getOtherClasses().forEach(otherClass ->
                otherClass.getDataRelationships().stream()
                        .map(DataRelationship::getCallee)
                        .forEach(dataClass -> dataConnections.get(dataClass).add(otherClass)));

        // Add outgoing relationships to data classes
        input.getDataClasses().forEach(dataClass ->
                dataClass.getDependenceRelationships().stream()
                        .map(DependenceRelationship::getCallee)
                        .filter(OtherClass.class::isInstance)
                        .map(OtherClass.class::cast)
                        .forEach(otherClass -> dataConnections.get(dataClass).add(otherClass)));

        // Add data class to data class relationships
        input.getDataClasses().forEach(dataClass ->
                dataClass.getDependenceRelationships().stream()
                        .map(DependenceRelationship::getCallee)
                        .filter(DataClass.class::isInstance)
                        .map(DataClass.class::cast)
                        .forEach(dataClass2 -> {
                            relatedDataClasses.get(dataClass).add(dataClass2);
                            relatedDataClasses.get(dataClass2).add(dataClass);
                        }));

        // Add other class to other class relationships from (recursive) data relationship classes
        input.getDataClasses().stream()
                .flatMap(dataClass -> getConnectedDataClasses(dataClass, relatedDataClasses).stream()
                        .map(dataConnections::get))
                .forEach(connectedOtherClasses -> connectedOtherClasses.forEach(
                        otherClass -> result.get(otherClass).addAll(connectedOtherClasses.stream()
                                // Make sure that we don't interpret an edge to itself.
                                .filter(connectedClass -> !connectedClass.equals(otherClass))
                                .collect(Collectors.toSet()))));

        return result;
    }

    /**
     * Returns the set of data classes that the given starting data class is implicitly and recursively connected to.
     *
     * @param startClass         the data class to define the connected data classes for
     * @param relatedDataClasses the structure of connected data classes
     * @return the data classes connected to the given data class
     */
    private Set<DataClass> getConnectedDataClasses(DataClass startClass, Map<DataClass, Set<DataClass>> relatedDataClasses) {
        var seen = new HashSet<DataClass>();
        var toVisit = new ArrayDeque<>(Set.of(startClass));
        while (!toVisit.isEmpty()) {
            var dataClass = toVisit.pop();
            if (seen.contains(dataClass)) {
                continue;
            }

            seen.add(dataClass);
            toVisit.addAll(relatedDataClasses.get(dataClass));
        }
        return seen;
    }

    @Getter
    @AllArgsConstructor
    private static class HierarchicalClustering {
        private final Clustering clustering;
        private final Solution solution;
    }
}
