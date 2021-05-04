package me.soels.tocairn.solver.ahca;

import me.soels.tocairn.model.*;
import me.soels.tocairn.solver.Clustering;
import me.soels.tocairn.solver.ClusteringBuilder;
import me.soels.tocairn.solver.Solver;
import me.soels.tocairn.solver.metric.MetricType;
import me.soels.tocairn.solver.metric.QualityCalculator;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static me.soels.tocairn.solver.metric.QualityCalculator.normalize;

/**
 * Solver implementation using the agglomorative hierarchical clustering algorithm (AHCA).
 */
public class AHCASolver implements Solver {
    private static final Logger LOGGER = LoggerFactory.getLogger(AHCASolver.class);
    private final AHCAConfiguration configuration;
    private Map<OtherClass, List<OtherClass>> classConnections;
    private final EvaluationInput input;
    private AHCAClustering initialClustering;

    public AHCASolver(AHCAConfiguration configuration, EvaluationInput input) {
        this.configuration = configuration;
        this.input = input;
    }

    @Override
    public void initialize(@Nullable Solution solution, boolean all) {
        classConnections = AHCAInitializer.calculateClassConnectedness(input);

        if (solution == null) {
            initialClustering = AHCAInitializer.createInitialSolution(configuration, input);
        } else {
            initialClustering = AHCAInitializer.createClusteringFromSolution(configuration, solution);
        }

    }

    @Override
    public AHCAEvaluationResult run() {
        LOGGER.info("Running agglomerative hierarchical clustering algorithm (AHCA)");
        var solutions = performAlgorithm();
        LOGGER.info("AHCA solver produced {} solutions", solutions.size());

        var result = new AHCAEvaluationResult();
        result.setSolutions(solutions);
        return result;
    }

    /**
     * Performs the agglomerative hierarchical clustering algorithm (AHCA) clustering.
     *
     * @return the preferable solution (if present)
     */
    // Suppress warnings as it is more readable in this case. Two retry conditions and one (unlikely) break condition.
    @SuppressWarnings("java:S135")
    private List<Solution> performAlgorithm() {
        // Config
        int minClusters = configuration.getMinClusterAmount().orElse(2);
        int maxClusters = configuration.getMaxClusterAmount().orElse(initialClustering.getClustering().getByClass().size());

        // State
        var solutionsToPersist = new ArrayList<Solution>();
        var previousClustering = initialClustering;
        var allCombinations = !configuration.isOptimizationOnSharedEdges();
        var startStep = initialClustering.getClustering().getByClass().size() - initialClustering.getClustering().getByCluster().size();
        var counter = startStep;
        var targetStep = initialClustering.getClustering().getByClass().size() - minClusters + 1;


        LOGGER.info("Going to start AHCA from step {} out of total {}.", counter, initialClustering.getClustering().getByClass().size());
        LOGGER.info("Going to continue until step {} based on {}.", targetStep, configuration.getMinClusterAmount().isPresent() ? "minClusterAmount" : "the default end of algorithm");
        logStep(counter, targetStep, System.currentTimeMillis(), "N/A", previousClustering);

        while (previousClustering.getClustering().getByCluster().size() > minClusters) {
            var iterationTimer = System.currentTimeMillis();

            // Get merger pairs
            var possiblePairs = getPossibleMergers(previousClustering, allCombinations);
            if (possiblePairs.isEmpty() && !allCombinations) {
                LOGGER.info("No more possible merger based on sharing edges. Retrying this iteration with, from now on, all combinations.");
                allCombinations = true;
                continue;
            } else if (possiblePairs.isEmpty()) {
                LOGGER.warn("No more possible mergers even though we consider all combinations. " +
                        "This should not happen due to stopping condition of minimum clusters defaulted to 2. " +
                        "Stopping algorithm.");
                break;
            }

            // Select best merger pair based on calculated metrics
            var best = getBestMerger(possiblePairs, previousClustering);

            // Retry iteration if quality got worse while we did not yet try all combinations.
            // Skip this for the first step when normalization is present as the first step can not be properly normalized.
            if (best.getTotalQuality() > previousClustering.getTotalQuality() && !allCombinations &&
                    (counter != startStep || !configuration.isNormalizeMetrics())) {
                LOGGER.warn("Non-normalized quality got worse between this and previous iteration: from {} to {} (delta {}).",
                        previousClustering.getTotalQuality(), best.getTotalQuality(), previousClustering.getTotalQuality() - best.getTotalQuality());
                LOGGER.info("Normalized quality changed from {} to {} (delta {}).",
                        previousClustering.getAssessedQuality(), best.getAssessedQuality(), previousClustering.getAssessedQuality() - best.getAssessedQuality());
                LOGGER.info("Retrying iteration with, from now on, considering all possible combinations.");
                allCombinations = true;
                continue;
            }

            // Persist if wanted and log step
            counter++;
            if (best.getClustering().getByCluster().size() <= maxClusters) {
                LOGGER.info("Persisted solution at step {}", counter);
                solutionsToPersist.add(best.getSolution());
            }
            logStep(counter, targetStep, iterationTimer, String.valueOf(possiblePairs.size()), best);

            // Set state for next iteration
            previousClustering = best;
        }
        return solutionsToPersist;
    }

    /**
     * Retrieves the best clustering from the given possible mergers for the current clustering based on the weighted
     * quality function from the configured metrics.
     *
     * @param possibleMergers    the possible pairs of microservices to merge
     * @param previousClustering the previous step's best clustering
     * @return this step's best clustering
     */
    private AHCAClustering getBestMerger(List<Pair<Integer, Integer>> possibleMergers,
                                         AHCAClustering previousClustering) {
        AtomicReference<AHCAClustering> bestClustering = new AtomicReference<>();

        var normalizationClusters = new ArrayList<Triple<Clustering, Double, Map<MetricType, double[]>>>();
        possibleMergers.parallelStream()
                .forEach(pair -> {
                    // Copy the graph, merge the clusters
                    var merger = new ClusteringBuilder(previousClustering.getClustering());
                    merger.mergeCluster(pair.getKey(), pair.getValue());
                    var clustering = merger.build();
                    var metrics = QualityCalculator.performMetrics(configuration, clustering);

                    // Calculate weighted total quality.
                    double weightedQuality = QualityCalculator.getWeightedTotalQuality(metrics, configuration.getWeights());

                    if (configuration.isNormalizeMetrics()) {
                        // When normalizing, store in a data structure. We will then recalculate the metrics in a later
                        // stage and reselect the best clustering. Note, as we persist the clustering outside the stream,
                        // this will not scale memory-wise due to many possible mergers (e.g. 100K+ or 1M+).
                        normalizationClusters.add(Triple.of(clustering, weightedQuality, metrics));
                    }
                    // Keep best.
                    checkAndSetBestQuality(bestClustering, clustering, weightedQuality, weightedQuality, metrics, Collections.emptyMap());
                });

        if (normalizationClusters.size() > 1) {
            // We should normalize. Recalculate metrics and select new best based the normalized weighted values.
            bestClustering.set(null);

            var minMaxValues = QualityCalculator.getMinMaxValues(normalizationClusters.stream()
                    .map(Triple::getRight)
                    .collect(Collectors.toList())
            );

            normalizationClusters.forEach(clusteringTriple -> {
                // Normalize the metrics for this clustering
                Map<MetricType, double[]> normalizedMetrics = normalize(clusteringTriple.getRight(), minMaxValues);
                // Calculate weighted total quality.
                double normalizedQuality = QualityCalculator.getWeightedTotalQuality(normalizedMetrics, configuration.getWeights());
                // Keep best.
                checkAndSetBestQuality(bestClustering, clusteringTriple.getLeft(), normalizedQuality, clusteringTriple.getMiddle(), clusteringTriple.getRight(), normalizedMetrics);
            });
        }

        return bestClustering.get();
    }

    /**
     * Synchronously checks whether the given clustering is the best clustering seen in this iteration. If so,
     * we keep it. This also creates the solution structure for persistence.
     * <p>
     * {@code assessedQuality} should be the normalized quality when normalization is present.
     *
     * @param currentBest     the currently known best clustering
     * @param clustering      the clustering to assess
     * @param assessedQuality the clustering's assessed quality.
     * @param totalQuality    the non-normalized quality.
     * @param metrics         the clustering's metrics to persist in the solution
     */
    private synchronized void checkAndSetBestQuality(AtomicReference<AHCAClustering> currentBest,
                                                     Clustering clustering,
                                                     double assessedQuality,
                                                     double totalQuality,
                                                     Map<MetricType, double[]> metrics,
                                                     Map<MetricType, double[]> normalized) {
        // As we negate maximization functions, the best quality is represented by the lowest number. Hence, lesser than.
        if (currentBest.get() == null || assessedQuality < currentBest.get().getAssessedQuality()) {
            var solution = new Solution();
            solution.setMetricValues(metrics);
            solution.setNormalizedMetricValues(normalized);
            solution.setMicroservices(clustering.getByCluster().entrySet().stream()
                    .map(cluster -> new Microservice(cluster.getKey(), cluster.getValue()))
                    .collect(Collectors.toList()));
            currentBest.set(new AHCAClustering(clustering, solution, assessedQuality, totalQuality));
        }
    }

    /**
     * Get the possible mergers for the current clustering.
     * <p>
     * If the provided {@code allCombinations} is false, this returns the combinations of microservices sharing an
     * (in)direct edge for which to select the best clustering. If it is true, it returns all possible combinations
     * for merging the microservices.
     * <p>
     * {@code allCombinations} should be set to {@code false} when it is configured in
     * {@link AHCAConfiguration#isOptimizationOnSharedEdges()}. It should be set to {@code true} in case (1)
     * the aforementioned configuration is set to {@code false}, (2) when quality is
     * decreasing in the current step and (3) when those sharing an edge returns an empty list.
     *
     * @param currentClustering the current step's clustering
     * @param allCombinations   whether to return combinations on all combinations instead of sharing an (in)direct edge
     * @return the possible mergers to assess in this iteration
     */
    private List<Pair<Integer, Integer>> getPossibleMergers(AHCAClustering currentClustering, boolean allCombinations) {
        if (allCombinations) {
            return getAllPossibleMergers(currentClustering.getClustering());
        } else {
            return getPossibleMergersWithSharedEdge(currentClustering.getClustering());
        }
    }

    /**
     * Computes the set of possible clusterings from the given clustering.
     * <p>
     * Note, similar to Clauset, Newman & Moore, here we only allow merging between microservices that share an edge.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     * @see AHCAInitializer
     */
    public List<Pair<Integer, Integer>> getPossibleMergersWithSharedEdge(Clustering currentClustering) {
        return currentClustering.getByCluster().entrySet().parallelStream()
                .flatMap(entry -> entry.getValue().stream()
                        .flatMap(clazz -> classConnections.get(clazz).stream())
                        .map(connectedClass -> currentClustering.getByClass().get(connectedClass))
                        .filter(ms -> !entry.getKey().equals(ms))
                        .map(ms -> entry.getKey() < ms ? Pair.of(entry.getKey(), ms) : Pair.of(ms, entry.getKey())))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Computes the set of all possible clusterings from the given clustering.
     * <p>
     * This creates a set of pairs of integers indicating which pair of microservices need to be merged based on their
     * labels.
     *
     * @param currentClustering the clustering to merge
     * @return all possible mergers for the given clustering
     * @see <a href="https://www.baeldung.com/java-combinations-algorithm">Baeldung Java combinations algorithm</a>
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

    private void logStep(int counter, int targetStep, long iterationTimer, String possiblePairs, AHCAClustering best) {
        if (LOGGER.isInfoEnabled()) {
            var duration = DurationFormatUtils.formatDuration(System.currentTimeMillis() - iterationTimer, "mm:ss.SSS");
            var qualityLog = configuration.isNormalizeMetrics() ?
                    String.format("Normalized quality: %.5f. Total quality: %.5f", best.getAssessedQuality(), best.getTotalQuality()) :
                    String.format("Quality: %.5f", best.getAssessedQuality());
            LOGGER.info("Step: {}/{}. Microservices: {}. {}. From possible mergers: {}. Duration: {} (m:s.millis)",
                    counter, targetStep, best.getClustering().getByCluster().size(), qualityLog, possiblePairs, duration);
        }
    }
}
