package me.soels.thesis.services;

import me.soels.thesis.clustering.ClusteringExecutorProvider;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.model.*;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        runWithExecutor(executor, evaluation.getId())
                .ifPresent(result -> processResult(evaluation, result));
    }

    private void processResult(Evaluation evaluation, NondominatedPopulation result) {
        var input = inputService.getInput(evaluation);
        var storedResult = new EvaluationResult();
        // TODO: Set metric information (runtime etc.).
        result.forEach(solution -> processSolution(input, evaluation.getConfiguration(), storedResult, solution));
        resultService.storeResult(evaluation, storedResult);
        LOGGER.info("Result {} stored in database", storedResult.getId());
    }

    private void processSolution(EvaluationInput input, EvaluationConfiguration configuration, EvaluationResult result, org.moeaframework.core.Solution solution) {
        var storedSolution = new Solution();
        var clustering = decoder.decode(solution, input, configuration);
        // TODO: Store objective values
        clustering.getByCluster().forEach((clusterNumber, classes) -> processCluster(storedSolution, clusterNumber, classes));
        resultService.storeSolution(result, storedSolution);
    }

    private void processCluster(Solution solution, int clusterNumber, List<? extends AbstractClass> classes) {
        var storedCluster = new Cluster();
        storedCluster.setClusterNumber(clusterNumber);
        storedCluster.getNodes().addAll(classes);
        resultService.storeCluster(solution, storedCluster);
    }

    private Optional<NondominatedPopulation> runWithExecutor(Executor executor, UUID evaluationId) {
        try {
            var result = executor.run();
            LOGGER.info("The evaluation with ID {} succeeded.", evaluationId);
            evaluationService.updateStatus(evaluationId, DONE);
            return Optional.of(result);
        } catch (Exception e) {
            LOGGER.error("The evaluation with ID " + evaluationId + " failed.", e);
            evaluationService.updateStatus(evaluationId, ERRORED);
            return Optional.empty();
        }
    }
}
