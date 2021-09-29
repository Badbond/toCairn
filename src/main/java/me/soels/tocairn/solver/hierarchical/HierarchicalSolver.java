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
import java.util.stream.Stream;

/**
 * Solver implementation using the agglomorative hierarchical clustering algorithm.
 */
public class HierarchicalSolver implements Solver {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalSolver.class);
    private final HierarchicalConfiguration configuration;
    private Map<OtherClass, Set<OtherClass>> classConnectionsThroughData;

    public HierarchicalSolver(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public HierarchicalEvaluationResult run(EvaluationInput input) {
        LOGGER.info("Running hierarchical solver");
        classConnectionsThroughData = calculateDataClassConnectedness(input);
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
    private Optional<HierarchicalClustering> getBestMerger(List<Pair<Integer, Integer>> possibleMergers, HierarchicalClustering currentClustering) {
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

    private List<Pair<Integer, Integer>> getPossibleMergers(HierarchicalClustering currentClustering) {
        var sharingAnEdge = getPossibleMergersWithSharedEdge(currentClustering.clustering);
        if (!sharingAnEdge.isEmpty()) {
            return sharingAnEdge;
        } else {
            return getAllPossibleMergers(currentClustering.clustering);
        }
    }

    /**
     * Computes the list of possible clusterings from the given clustering.
     * <p>
     * Returns an empty list if {@link HierarchicalConfiguration#isOptimizationOnSharedEdges()} is set to {@code false}.
     * <p>
     * Note, similar to Clauset, Newman & Moore, here we only allow merging between microservices that share an edge.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     */
    public List<Pair<Integer, Integer>> getPossibleMergersWithSharedEdge(Clustering currentClustering) {
        if (!configuration.isOptimizationOnSharedEdges()) {
            return Collections.emptyList();
        }

        var dataConnectedMicroservices = new HashMap<DataClass, List<Integer>>();
        currentClustering.getByCluster()
                .forEach((microserviceLabel, microservice) -> microservice.stream()
                        .flatMap(otherClass -> otherClass.getDataRelationships().stream())
                        .map(DataRelationship::getCallee)
                        .forEach(dataClass -> {
                            var existing = dataConnectedMicroservices.computeIfAbsent(dataClass, d -> new ArrayList<>());
                            existing.add(microserviceLabel);
                        })
                );

        var directStream = currentClustering.getByCluster().entrySet().parallelStream()
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                        .map(rel -> currentClustering.getByClass().get(rel.getCallee()))
                        .distinct()
                        .filter(key -> !entry.getKey().equals(key))
                        .map(other -> other < entry.getKey() ? other + ":" + entry.getKey() : entry.getKey() + ":" + other));

        var viaDataStream = currentClustering.getByCluster().entrySet().parallelStream()
                .flatMap(entry -> {
                    var connectedMss = findOtherMicroservicesViaDataRelationship(entry.getValue(), currentClustering);
                    return connectedMss.stream()
                            .distinct()
                            // Do not include self.
                            .filter(ms -> !entry.getKey().equals(ms))
                            // Sort keys from smaller to greater for removing duplicates.
                            .map(ms -> ms < entry.getKey() ? ms + ":" + entry.getKey() : entry.getKey() + ":" + ms);
                });

        return Stream.concat(directStream, viaDataStream)
                .map(key -> key.split(":"))
                .map(parts -> Pair.of(Integer.valueOf(parts[0]), Integer.valueOf(parts[1])))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Integer> findOtherMicroservicesViaDataRelationship(List<OtherClass> microservice, Clustering clustering) {
        return microservice.stream()
                .flatMap(clazz -> clazz.getDataRelationships().stream())
                .map(DataRelationship::getCallee)
                .flatMap(dataClass -> traverseDataClass(dataClass, clustering, new ArrayList<>()).stream())
                .collect(Collectors.toList());
    }

    private List<Integer> traverseDataClass(DataClass dataClass, Clustering clustering, List<DataClass> seen) {
        if (seen.contains(dataClass)) {
            return Collections.emptyList();
        }
        seen.add(dataClass);
        var microservices = new ArrayList<Integer>();
        for (var depRel : dataClass.getDependenceRelationships()) {
            if (depRel.getCallee() instanceof OtherClass) {
                microservices.add(clustering.getByClass().get(depRel.getCallee()));
            } else {
                microservices.addAll(traverseDataClass((DataClass) depRel.getCallee(), clustering, seen));
            }
        }
        // TODO: Check recursive dependencies
        return microservices;
    }

    /**
     * Computes the list of possible clusterings from the given clustering.
     * <p>
     * Note, similar to Clauset, Newman & Moore, here we only allow merging between microservices that share an edge.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     */
    public List<Pair<Integer, Integer>> getAllPossibleMergers(Clustering currentClustering) {
        List<int[]> combinations = new ArrayList<>();
        helper(combinations, new int[2], 0, currentClustering.getByCluster().size() - 1, 0);

        return combinations.stream()
                .map(combination -> Pair.of(combination[0], combination[1]))
                .collect(Collectors.toList());
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

    private Map<OtherClass, Set<OtherClass>> calculateDataClassConnectedness(EvaluationInput input) {
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
                        .forEach(dataClass2 -> relatedDataClasses.get(dataClass).add(dataClass2)));


        // Add  other class to other class relationships from (recursive) data relationship classes
        input.getDataClasses().stream()
                .map(dataClass -> getRecursiveOtherClassesForDataClass(dataClass, dataConnections, relatedDataClasses))
                .forEach(connectedOtherClasses -> connectedOtherClasses.forEach(
                        otherClass -> result.get(otherClass).addAll(connectedOtherClasses.stream()
                                .filter(connectedClass -> !connectedClass.equals(otherClass))
                                .collect(Collectors.toSet()))));

        return result;
    }

    private Set<OtherClass> getRecursiveOtherClassesForDataClass(DataClass dataClass,
                                                                 Map<DataClass, Set<OtherClass>> dataConnections,
                                                                 Map<DataClass, Set<DataClass>> relatedDataClasses) {
        // TODO: Implement
        return Collections.emptySet();
    }

    private Set<DataClass> getConnectedDataClass(DataClass dataClass, Map<DataClass, Set<DataClass>> relatedDataClasses) {
        // TODO: Implement
        return Collections.emptySet();
    }

    @Getter
    @AllArgsConstructor
    private static class HierarchicalClustering {
        private final Clustering clustering;
        private final Solution solution;
    }
}
