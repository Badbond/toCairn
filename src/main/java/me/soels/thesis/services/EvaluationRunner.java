package me.soels.thesis.services;

import me.soels.thesis.clustering.ClusteringContextProvider;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.model.*;
import org.moeaframework.Analyzer;
import org.moeaframework.Analyzer.AnalyzerResults;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static me.soels.thesis.model.EvaluationStatus.DONE;
import static me.soels.thesis.model.EvaluationStatus.ERRORED;

/**
 * Service responsible for performing a run for a configured evaluation and storing the result.
 */
@Service
public class EvaluationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationRunner.class);
    private final ClusteringContextProvider clusteringContextProvider;
    private final VariableDecoder decoder;
    private final EvaluationService evaluationService;
    private final AtomicBoolean runnerRunning = new AtomicBoolean(false);

    public EvaluationRunner(ClusteringContextProvider clusteringContextProvider,
                            VariableDecoder decoder,
                            EvaluationService evaluationService) {
        this.clusteringContextProvider = clusteringContextProvider;
        this.decoder = decoder;
        this.evaluationService = evaluationService;
    }

    @Async
    public void runEvaluation(Evaluation evaluation) {
        if (!runnerRunning.compareAndSet(false, true)) {
            throw new IllegalArgumentException("An static analysis is already running. The JavaParser library has " +
                    "degraded coverage due to errors when running in parallel. Stopping analysis");
        }

        try {
            LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
            var input = new EvaluationInputBuilder(evaluation.getInputs()).build();
            var problem = clusteringContextProvider.createProblem(evaluation, input);
            var executor = clusteringContextProvider.createExecutor(problem, evaluation.getConfiguration());
            runWithExecutor(executor, evaluation)
                    .ifPresent(result -> {
                        var analyzer = clusteringContextProvider.createAnalyzer(result, executor,
                                evaluation.getConfiguration().getAlgorithm());
                        storeResult(evaluation, input, result, analyzer.getAnalysis());
                    });
        } finally {
            runnerRunning.set(false);
        }
    }

    /**
     * Stores the MOEA algorithm result with its solutions, objective values and clusters for the given evaluation.
     *
     * @param evaluation      the evaluation to store the result in
     * @param input           the input graph for decoding purposes
     * @param result          the MOEA population result
     * @param analysisResults the analyzer containing MOEA metrics
     */
    private void storeResult(Evaluation evaluation,
                             EvaluationInput input,
                             NondominatedPopulation result,
                             AnalyzerResults analysisResults) {
        var newResult = new EvaluationResult();
        newResult.setCreatedDate(ZonedDateTime.now());

        analysisResults.getAlgorithms().stream()
                .map(analysisResults::get)
                .flatMap(algorithmResult ->
                        algorithmResult.getIndicators().stream()
                                .map(algorithmResult::get))
                .forEach(indicator -> setMetric(newResult, indicator));

        // TODO: Set metric information (runtime etc.).

        var solutions = StreamSupport.stream(result.spliterator(), false)
                .map(solution -> setupSolution(evaluation, input, solution))
                .collect(Collectors.toList());
        newResult.getSolutions().addAll(solutions);
        evaluation.getResults().add(newResult);
        evaluationService.save(evaluation);
        LOGGER.info("Evaluation run complete. Result id: {}, Solutions: {}, Clusters: {}", newResult.getId(),
                solutions.size(), solutions.stream().mapToInt(sol -> sol.getClusters().size()).sum());
    }

    private void setMetric(EvaluationResult newResult, Analyzer.IndicatorResult indicator) {
        switch (indicator.getIndicator()) {
            case "Hypervolume":
                newResult.setHyperVolume(indicator.getValues()[0]);
                break;
            case "GenerationalDistance":
                newResult.setGenerationalDistance(indicator.getValues()[0]);
                break;
            case "InvertedGenerationalDistance":
                newResult.setInvertedGenerationalDistance(indicator.getValues()[0]);
                break;
            case "AdditiveEpsilonIndicator":
                newResult.setAdditiveEpsilonIndicator(indicator.getValues()[0]);
                break;
            case "MaximumParetoFrontError":
                newResult.setMaximumParetoFrontError(indicator.getValues()[0]);
                break;
            case "Spacing":
                newResult.setSpacing(indicator.getValues()[0]);
                break;
            case "Contribution":
                newResult.setContribution(indicator.getValues()[0]);
                break;
            case "R1Indicator":
                newResult.setR1Indicator(indicator.getValues()[0]);
                break;
            case "R2Indicator":
                newResult.setR2Indicator(indicator.getValues()[0]);
                break;
            case "R3Indicator":
                newResult.setR3Indicator(indicator.getValues()[0]);
                break;
            default:
                LOGGER.warn("Unknown metric {} found. Not storing this metric and its values {}", indicator.getIndicator(), indicator.getValues());
        }
    }

    /**
     * Sets up a single solution and its clusters.
     *
     * @param evaluation the evaluation
     * @param input      the input to use in decoding the variables in the given solution
     * @param solution   the MOEA solution to convert
     * @return the yet unpersisted solution
     */
    private Solution setupSolution(Evaluation evaluation,
                                   EvaluationInput input,
                                   org.moeaframework.core.Solution solution) {
        var newSolution = new Solution();
        var i = 0;
        // Retrieve metric information
        for (var objective : evaluation.getObjectives()) {
            var metrics = clusteringContextProvider.getMetricsForObjective(objective);
            var values = Arrays.copyOfRange(solution.getObjectives(), i, i + metrics.size());
            newSolution.getObjectiveValues().put(objective, values);
            i += metrics.size();
        }

        // Set up clusters
        var clustering = decoder.decode(solution, input, evaluation.getConfiguration());
        clustering.getByCluster().entrySet().stream()
                .map(entry -> new Cluster(entry.getKey(), entry.getValue()))
                .forEach(cluster -> newSolution.getClusters().add(cluster));
        return newSolution;
    }

    private Optional<NondominatedPopulation> runWithExecutor(Executor executor, Evaluation evaluation) {
        try {
            // TODO: There is also a runWithSeeds to perform multiple runs.. should I include that maybe?
            var result = executor.run();
            LOGGER.info("The evaluation with ID {} succeeded.", evaluation.getId());
            evaluationService.updateStatus(evaluation, DONE);
            return Optional.of(result);
        } catch (Exception e) {
            LOGGER.error("The evaluation with ID " + evaluation.getId() + " failed.", e);
            evaluationService.updateStatus(evaluation, ERRORED);
            return Optional.empty();
        }
    }
}
