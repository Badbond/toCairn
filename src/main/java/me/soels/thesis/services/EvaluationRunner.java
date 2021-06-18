package me.soels.thesis.services;

import me.soels.thesis.clustering.ClusteringExecutorProvider;
import me.soels.thesis.clustering.encoding.Clustering;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.model.*;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
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
    private final ClusteringExecutorProvider executorProvider;
    private final VariableDecoder decoder;
    private final EvaluationResultService resultService;
    private final EvaluationInputService inputService;
    private final EvaluationService evaluationService;

    public EvaluationRunner(ClusteringExecutorProvider executorProvider,
                            VariableDecoder decoder,
                            EvaluationResultService resultService,
                            EvaluationInputService inputService,
                            EvaluationService evaluationService) {
        this.executorProvider = executorProvider;
        this.decoder = decoder;
        this.resultService = resultService;
        this.inputService = inputService;
        this.evaluationService = evaluationService;
    }

    @Async
    public void runEvaluation(Evaluation evaluation) {
        LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
        var input = inputService.getInput(evaluation);
        var executor = executorProvider.getExecutor(evaluation, input);
        runWithExecutor(executor, evaluation)
                .ifPresent(result -> storeResult(evaluation, result));
    }

    /**
     * Stores the MOEA algorithm result with its solutions, objective values and clusters for the given evaluation.
     *
     * @param evaluation the evaluation to store the result in
     * @param result     the MOEA population result
     */
    private void storeResult(Evaluation evaluation, NondominatedPopulation result) {
        var input = inputService.getInput(evaluation);
        var newResult = new EvaluationResult();
        // TODO: Set metric information (runtime etc.).

        var solutions = StreamSupport.stream(result.spliterator(), false)
                .map(solution -> setupSolution(evaluation, input, solution))
                .collect(Collectors.toList());
        resultService.storeSolutions(newResult, solutions);
        LOGGER.info("Stored {} solutions", solutions.size());

        var storedResult = resultService.storeResult(evaluation, newResult);
        LOGGER.info("Stored result {}", storedResult.getId());
    }

    /**
     * Sets up a single solution with the already persisted clusterings for the solution.
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
        for (var objective : evaluation.getObjectives()) {
            var metrics = executorProvider.getMetricsForObjective(objective);
            var values = Arrays.copyOfRange(solution.getObjectives(), i, i + metrics.size());
            newSolution.getObjectiveValues().put(objective, values);
            i += metrics.size();
        }

        var clustering = decoder.decode(solution, input, evaluation.getConfiguration());
        storeClusters(newSolution, clustering);
        return newSolution;
    }

    /**
     * Converts the clustering to the persisted clusters and sets it on the given solution.
     *
     * @param solution   the solution to apply the clusters to
     * @param clustering the clustering to created clusters from
     */
    private void storeClusters(Solution solution, Clustering clustering) {
        var clusters = clustering.getByCluster().entrySet().stream()
                .map(entry -> new Cluster(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        resultService.storeClusters(solution, clusters);
        LOGGER.info("Stored {} clusters", clusters.size());
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
