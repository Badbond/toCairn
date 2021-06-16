package me.soels.thesis.services;

import me.soels.thesis.clustering.ClusteringExecutorProvider;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.ArrayUtils;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static me.soels.thesis.model.EvaluationStatus.DONE;
import static me.soels.thesis.model.EvaluationStatus.ERRORED;

/**
 * Service responsible for performing a run for a configured evaluation.
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
                .ifPresent(result -> processResult(evaluation, result));
    }

    private void processResult(Evaluation evaluation, NondominatedPopulation result) {
        var input = inputService.getInput(evaluation);
        var storedResult = new EvaluationResult();
        // TODO: Set metric information (runtime etc.).
        result.forEach(solution -> processSolution(evaluation, input, storedResult, solution));
        resultService.storeResult(evaluation, storedResult);
        LOGGER.info("Result {} stored in database", storedResult.getId());
    }

    private void processSolution(Evaluation evaluation, EvaluationInput input, EvaluationResult result, org.moeaframework.core.Solution solution) {
        var storedSolution = new Solution();
        var i = 0;
        for (var objective : evaluation.getObjectives()) {
            var metrics = executorProvider.getMetricsForObjective(objective);
            var values = Arrays.asList(ArrayUtils.toObject(
                    Arrays.copyOfRange(solution.getObjectives(), i, i + metrics.size())));
            storedSolution.getObjectiveValues().put(objective, values);
            i += metrics.size();
        }
        var clustering = decoder.decode(solution, input, evaluation.getConfiguration());
        clustering.getByCluster().forEach((clusterNumber, classes) -> processCluster(storedSolution, clusterNumber, classes));
        resultService.storeSolution(result, storedSolution);
    }

    private void processCluster(Solution solution, int clusterNumber, List<? extends AbstractClass> classes) {
        var storedCluster = new Cluster();
        storedCluster.setClusterNumber(clusterNumber);
        storedCluster.getNodes().addAll(classes);
        resultService.storeCluster(solution, storedCluster);
    }

    private Optional<NondominatedPopulation> runWithExecutor(Executor executor, Evaluation evaluation) {
        try {
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
