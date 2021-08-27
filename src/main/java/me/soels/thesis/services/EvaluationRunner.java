package me.soels.thesis.services;

import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInputBuilder;
import me.soels.thesis.model.EvaluationStatus;
import me.soels.thesis.solver.SolverFactory;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for performing a run for a configured evaluation and storing the result.
 */
@Service
public class EvaluationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationRunner.class);
    private final EvaluationService evaluationService;
    private final SolverFactory solverFactory;
    private final AtomicBoolean runnerRunning = new AtomicBoolean(false);

    public EvaluationRunner(EvaluationService evaluationService, SolverFactory solverFactory) {
        this.evaluationService = evaluationService;
        this.solverFactory = solverFactory;
    }

    @Async
    public void runEvaluation(Evaluation evaluation) {
        if (!runnerRunning.compareAndSet(false, true)) {
            throw new IllegalArgumentException("An static analysis is already running. The JavaParser library has " +
                    "degraded coverage due to errors when running in parallel. Stopping analysis");
        }

        try {
            var input = new EvaluationInputBuilder(evaluation.getInputs()).build();
            var solver = solverFactory.createSolver(evaluation, input);

            var start = ZonedDateTime.now();
            LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
            var result = solver.run(input);
            evaluation.setStatus(EvaluationStatus.DONE);

            result.setStartDate(start);
            result.setFinishDate(ZonedDateTime.now());
            evaluation.getResults().add(result);
            evaluation = evaluationService.save(evaluation);

            var duration = DurationFormatUtils.formatDurationHMS(ChronoUnit.MILLIS.between(start, result.getFinishDate()));
            LOGGER.info("Evaluation run complete. Took {} (H:m:s.millis)", duration);
            LOGGER.info("Result id: {}, Solutions: {}, Clusters: {}", result.getId(), result.getSolutions().size(),
                    result.getSolutions().stream().mapToInt(sol -> sol.getMicroservices().size()).sum());
        } catch (Exception e) {
            LOGGER.error("The evaluation with ID " + evaluation.getId() + " failed.", e);
            evaluationService.updateStatus(evaluation.getId(), EvaluationStatus.ERRORED);
        } finally {
            runnerRunning.set(false);
        }
    }
}
