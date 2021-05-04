package me.soels.tocairn.solver.moeca;

import me.soels.tocairn.model.*;
import me.soels.tocairn.solver.Solver;
import me.soels.tocairn.solver.metric.MetricType;
import me.soels.tocairn.solver.metric.QualityCalculator;
import org.apache.commons.lang3.tuple.Pair;
import org.moeaframework.Analyzer;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.spi.AlgorithmFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static me.soels.tocairn.util.Constants.NSGAII_STRING;

/**
 * Solver implementation using a multi-objective evolutionary clustering algorithm (MOECA).
 *
 * @see MOECAProblem
 */
public class MOECASolver implements Solver {
    private final MOECAConfiguration configuration;
    private final List<MetricType> metricTypes;
    private final EvaluationInput input;
    private final MOECAExecutor executor;
    private final VariableDecoder decoder;

    public MOECASolver(MOECAConfiguration configuration,
                       List<MetricType> metricTypes,
                       EvaluationInput input,
                       MOECAExecutor executor,
                       VariableDecoder decoder) {
        this.configuration = configuration;
        this.metricTypes = metricTypes;
        this.input = input;
        this.executor = executor;
        this.decoder = decoder;
    }

    @Override
    public void initialize(@Nullable Solution solution, boolean all) {
        if (solution == null) {
            return; // Defaults to random population
        }
        if (configuration.getEncodingType() != EncodingType.CLUSTER_LABEL) {
            throw new IllegalArgumentException("Can only add solution to initial population when using " + EncodingType.CLUSTER_LABEL + " encoding");
        }
        if (!NSGAII_STRING.equals(configuration.getAlgorithm())) {
            throw new IllegalArgumentException("Can only add solution to initial population when using the " + NSGAII_STRING + " algorithm");
        }

        var algorithmFactory = AlgorithmFactory.getInstance();
        algorithmFactory.addProvider(new InjectedSolutionNSGAIIAlgorithmProvider(input, solution, all));
        executor.usingAlgorithmFactory(algorithmFactory);
    }

    @Override
    public MOECAEvaluationResult run() {
        var population = executor.run();
        var analysis = performAnalysis(population, executor, configuration.getAlgorithm());
        return createResult(population, analysis, executor.getProblem().getMinMaxValues());
    }

    /**
     * Creates a {@link MOECAEvaluationResult} from the given {@link NondominatedPopulation} and additional analysis
     * information.
     *
     * @param population      the resulting non dominated population
     * @param analysisResults the additional analysis metadata
     * @param minMaxValues    the metric min-max values to perform normalization
     * @return the result for running MOECA
     */
    private MOECAEvaluationResult createResult(NondominatedPopulation population,
                                               Analyzer.AlgorithmResult analysisResults,
                                               Map<MetricType, Pair<Double, Double>[]> minMaxValues) {
        var result = new MOECAEvaluationResult();

        // Add solutions
        var solutions = StreamSupport.stream(population.spliterator(), false)
                .map(solution -> createSolutionFromResult(input, solution, minMaxValues))
                .collect(Collectors.toList());
        result.getSolutions().addAll(solutions);

        // Add population metrics
        analysisResults.getIndicators().stream()
                .map(analysisResults::get)
                .forEach(indicator -> result.getPopulationMetrics().put(indicator.getIndicator(), indicator.getValues()[0]));

        // Add minmax values
        result.setMinMetricValues(minMaxValues.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), Arrays.stream(entry.getValue())
                        .mapToDouble(Pair::getKey)
                        .toArray()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        );
        result.setMaxMetricValues(minMaxValues.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), Arrays.stream(entry.getValue())
                        .mapToDouble(Pair::getValue)
                        .toArray()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
        );

        return result;
    }

    /**
     * Returns the result of running the analyzer on the result.
     *
     * @param result    the non dominated population result from the MOECA
     * @param executor  the executor used in MOECA
     * @param algorithm the name of the algorithm used in MOECA
     * @return the metrics for the population
     */
    private Analyzer.AlgorithmResult performAnalysis(NondominatedPopulation result, Executor executor, String algorithm) {
        return new Analyzer()
                .withSameProblemAs(executor)
                .add(algorithm, result)
                .includeAllMetrics()
                .showAll()
                .getAnalysis()
                .get(algorithm);
    }

    /**
     * Sets up a single solution and its clusters from the MOECA solution output.
     *
     * @param input        the input to use in decoding the variables in the given solution
     * @param solution     the MOECA solution to convert
     * @param minMaxValues the metric min-max values to normalize solution metrics with
     * @return the yet unpersisted solution
     */
    private Solution createSolutionFromResult(EvaluationInput input,
                                              org.moeaframework.core.Solution solution,
                                              Map<MetricType, Pair<Double, Double>[]> minMaxValues) {
        var newSolution = new Solution();

        var i = 0;
        var normalizationMap = new EnumMap<MetricType, double[]>(MetricType.class);
        for (var metricType : metricTypes) {
            var metrics = metricType.getMetrics();
            var values = Arrays.copyOfRange(solution.getObjectives(), i, i + metrics.size());
            newSolution.getMetricValues().put(metricType, values);
            normalizationMap.put(metricType, values);
            i += metricType.getMetrics().size();
        }
        newSolution.setNormalizedMetricValues(QualityCalculator.normalize(normalizationMap, minMaxValues));

        // Set up clusters
        var clustering = decoder.decode(solution, input, configuration, null);
        clustering.getByCluster().entrySet().stream()
                .map(entry -> new Microservice(entry.getKey(), entry.getValue()))
                .forEach(microservice -> newSolution.getMicroservices().add(microservice));

        return newSolution;
    }
}
