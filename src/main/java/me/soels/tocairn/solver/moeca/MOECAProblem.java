package me.soels.tocairn.solver.moeca;

import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.MOECAConfiguration;
import me.soels.tocairn.solver.OptimizationData;
import me.soels.tocairn.solver.metric.MetricType;
import org.apache.commons.lang3.tuple.Pair;
import org.moeaframework.Executor;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.distributed.DistributedProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Models the multi objective evolutionary clustering algorithm's problem.
 * <p>
 * This model is responsible for defining the encoding of genes used by the evolutionary algorithm. This is configurable
 * through the {@link EncodingType} provided.
 * <p>
 * This problem can be parallelized as this problem only maintains state that does not change between evaluations. One
 * can do so using {@link DistributedProblem} or {@link Executor#distributeOnAllCores()}.
 *
 * @see EncodingType
 */
public class MOECAProblem extends AbstractProblem {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOECAProblem.class);
    private final List<MetricType> metrics;
    private final Map<MetricType, Pair<Double, Double>[]> minMaxValues;
    private final EvaluationInput evaluationInput;
    private final MOECAConfiguration configuration;
    private final VariableDecoder variableDecoder;
    private final OptimizationData optimizationData = new OptimizationData();
    private final AtomicInteger evaluationCounter = new AtomicInteger(0);
    private final AtomicInteger deniedCounter = new AtomicInteger(0);

    /**
     * Constructs a new instance of the clustering problem
     *
     * @param analysisInput   the input to cluster
     * @param configuration   the configuration for the problem
     * @param variableDecoder the decoder service to decode the solution with
     */
    @SuppressWarnings("unchecked") // Generic array creation, safe cast.
    public MOECAProblem(EvaluationInput analysisInput, MOECAConfiguration configuration, VariableDecoder variableDecoder) {
        super(analysisInput.getOtherClasses().size(), configuration.getMetrics().stream()
                .mapToInt(metricType -> metricType.getMetrics().size())
                .sum());
        this.metrics = configuration.getMetrics();
        this.evaluationInput = analysisInput;
        this.configuration = configuration;
        this.variableDecoder = variableDecoder;
        this.minMaxValues = metrics.stream()
                .collect(Collectors.toMap(metricType -> metricType, metricType ->
                        metricType.getMetrics().stream()
                                .map(metric -> Pair.of(Double.MAX_VALUE, Double.MIN_VALUE))
                                .toArray(Pair[]::new))
                );
    }

    /**
     * Evaluates a solution based on the configured metrics.
     * <p>
     * Decodes the given solution to a structure understandable for the metrics, evaluates the metrics,
     * and sets the objective values for these metrics on the solution.
     *
     * @param solution the solution to evaluate
     */
    @Override
    public void evaluate(Solution solution) {
        var decodedClustering = variableDecoder.decode(solution, evaluationInput, configuration, optimizationData);

        if (configuration.getMinClusterAmount().isPresent() &&
                decodedClustering.getByCluster().size() < configuration.getMinClusterAmount().get()) {
            solution.setConstraint(0, -1); // Too few clusters
            deniedCounter.incrementAndGet();
            return;
        } else if (configuration.getMaxClusterAmount().isPresent() &&
                decodedClustering.getByCluster().size() > configuration.getMaxClusterAmount().get()) {
            solution.setConstraint(0, 1); // Too many clusters
            deniedCounter.incrementAndGet();
            return;
        }

        // MOEAFramework works with Future<Solution> which does not allow us to call solution.getObjectives().
        // This data object allows us to log the metrics without having to call that function.
        var metricValues = new HashMap<String, Double>();

        var objectiveCounter = 0;
        for (var metricType : metrics) {
            for (int i = 0; i < metricType.getMetrics().size(); i++) {
                var metric = metricType.getMetrics().get(i);
                double metricValue = metric.calculate(decodedClustering);
                solution.setObjective(objectiveCounter++, metricValue);

                // Add to minmax
                var existing = minMaxValues.get(metricType)[i];
                if (metricValue < existing.getKey()) {
                    minMaxValues.get(metricType)[i] = Pair.of(metricValue, existing.getValue());
                }
                if (metricValue > existing.getValue()) {
                    minMaxValues.get(metricType)[i] = Pair.of(existing.getKey(), metricValue);
                }

                metricValues.put(metric.getClass().getSimpleName(), metricValue);
            }
        }

        if (optimizationData.hasTooMuchCached()) {
            // Too large applications causes too much caching and therefore memory problems.
            optimizationData.clearCache();
        }

        var counter = evaluationCounter.incrementAndGet();
        if (counter % 1000 == 0 && LOGGER.isInfoEnabled()) {
            var denied = deniedCounter.getAndSet(0);
            LOGGER.info("Performed {}/{} evaluations. Denied {}.", counter, configuration.getMaxEvaluations(), denied);
            LOGGER.info("Metric values for solution {} for metrics {} resp.",
                    metricValues.values().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ", "[", "]")),
                    metricValues.keySet().stream()
                            .collect(Collectors.joining(", ", "[", "]")));
        }
    }

    /**
     * Constructs the solution structure.
     * <p>
     * This does not do initialization of the initial population as that is depending on the algorithm. This therefore
     * only defines the structure of encoding used. Both cluster-label encoding and locus-adjacency graph encoding
     * have the same typing in terms of variables.
     * <p>
     * Regarding the bounds of the variables, we can not need to place bounds as we normalize our clustering during
     * decoding such that it has increasing cluster numbers. Our bounds are then validates with constraints.
     * They therefore have the same bounds as in cluster-label encoding there can be 1 up to n number of clusters and
     * in locus-adjacency graph encoding every class can be linked to any of the n classes, where n is the amount of
     * classes.
     *
     * @return the solution structure
     * @see EncodingType
     */
    @Override
    public Solution newSolution() {
        // The constraint that we evaluate if the number of desired clusters.
        var solution = new Solution(getNumberOfVariables(), getNumberOfObjectives(), 1);

        for (var i = 0; i < getNumberOfVariables(); i++) {
            // We use floats instead of binary integers as those allow for more mutation/crossover operations,
            // Preliminary investigation showed there is not much of a performance increase into using binary integers
            solution.setVariable(i, EncodingUtils.newInt(0, getUpperbound()));
        }
        return solution;
    }

    private int getUpperbound() {
        if (configuration.getEncodingType() == EncodingType.CLUSTER_LABEL) {
            return Integer.min(
                    configuration.getMaxClusterAmount().orElse(getNumberOfVariables()),
                    getNumberOfVariables())
                    - 1;
        }
        return getNumberOfVariables() - 1;
    }

    public Map<MetricType, Pair<Double, Double>[]> getMinMaxValues() {
        return minMaxValues;
    }
}
