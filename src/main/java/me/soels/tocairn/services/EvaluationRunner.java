package me.soels.tocairn.services;

import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationInputBuilder;
import me.soels.tocairn.model.EvaluationStatus;
import me.soels.tocairn.solver.SolverFactory;
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
    public void runEvaluation(Evaluation evaluation) {
        try {
            if (!runnerRunning.compareAndSet(false, true)) {
                throw new IllegalArgumentException("An evaluation is already running. To make sure an analysis receives " +
                        "all the needed resources, we don't allow to run multiple in parallel.");
            }

            var input = new EvaluationInputBuilder(evaluation.getInputs()).build();
            var solver = solverFactory.createSolver(evaluation, input);

            var start = ZonedDateTime.now();
            LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
            var result = solver.run(input);
            evaluation.setStatus(EvaluationStatus.DONE);

            result.setStartDate(start);
            result.setFinishDate(ZonedDateTime.now());
            resultService.persistResult(result);
            evaluationService.createResultRelationship(evaluation.getId(), result.getId());

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
