package me.soels.thesis.services;

import me.soels.thesis.clustering.ClusteringExecutorProvider;
import me.soels.thesis.model.Evaluation;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static me.soels.thesis.model.EvaluationStatus.DONE;
import static me.soels.thesis.model.EvaluationStatus.ERRORED;

/**
 * Service responsible for performing a run for a configured evaluation.
 */
@Service
public class EvaluationRunner {
    private final ClusteringExecutorProvider executorProvider;
    private final GraphService graphService;
    private final EvaluationService evaluationService;
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationRunner.class);

    public EvaluationRunner(ClusteringExecutorProvider executorProvider,
                            GraphService graphService,
                            EvaluationService evaluationService) {
        this.executorProvider = executorProvider;
        this.graphService = graphService;
        this.evaluationService = evaluationService;
    }

    @Async
    public void runEvaluation(Evaluation evaluation) {
        LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
        var input = graphService.getInput(evaluation.getId());
        var executor = executorProvider.getExecutor(evaluation, input);
        var result = runWithExecutor(executor, evaluation.getId());
        // TODO: Set metric information (runtime etc.).
        // TODO: Store results (in separate service?)
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
