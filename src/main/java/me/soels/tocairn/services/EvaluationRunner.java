package me.soels.tocairn.services;

import me.soels.tocairn.model.*;
import me.soels.tocairn.solver.SolverFactory;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
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
    private final EvaluationResultService resultService;
    private final SolverFactory solverFactory;
    private final AtomicBoolean runnerRunning = new AtomicBoolean(false);

    public EvaluationRunner(EvaluationService evaluationService,
                            EvaluationResultService resultService,
                            SolverFactory solverFactory) {
        this.evaluationService = evaluationService;
        this.resultService = resultService;
        this.solverFactory = solverFactory;
    }

    @Async
    public void runEvaluation(Evaluation evaluation, String name, @Nullable Solution solution, boolean all) {
        try {
            // Lock.
            if (!runnerRunning.compareAndSet(false, true)) {
                throw new IllegalArgumentException("An evaluation is already running. To make sure an analysis receives " +
                        "all the needed resources, we don't allow to run multiple in parallel.");
            }

            run(evaluation, name, solution, all);
        } catch (Exception e) {
            LOGGER.error("The evaluation with ID " + evaluation.getId() + " failed.", e);
            evaluationService.updateStatus(evaluation.getId(), EvaluationStatus.ERRORED);
        } finally {
            // Unlock.
            runnerRunning.set(false);
        }
    }

    private void run(Evaluation evaluation, String name, @Nullable Solution solution, boolean all) {
        var input = new EvaluationInputBuilder(evaluation.getInputs()).build();
        var solver = solverFactory.createSolver(evaluation, input);
        solver.initialize(solution, all);

        var start = ZonedDateTime.now();
        LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());

        var result = solver.run();
        persistResult(evaluation, name, start, result);

        var duration = DurationFormatUtils.formatDurationHMS(ChronoUnit.MILLIS.between(start, result.getFinishDate()));
        LOGGER.info("Evaluation run complete. Took {} (H:m:s.millis)", duration);
        evaluationService.updateStatus(evaluation.getId(), EvaluationStatus.DONE);
        LOGGER.info("Result id: {}, Solutions: {}, Clusters: {}", result.getId(), result.getSolutions().size(),
                result.getSolutions().stream().mapToInt(sol -> sol.getMicroservices().size()).sum());
    }

    private void persistResult(Evaluation evaluation, String name, ZonedDateTime start, EvaluationResult result) {
        result.setStartDate(start);
        result.setFinishDate(ZonedDateTime.now());
        result.setName(name);

        resultService.persistResult(result);
        evaluationService.createResultRelationship(evaluation.getId(), result.getId());
    }
}
