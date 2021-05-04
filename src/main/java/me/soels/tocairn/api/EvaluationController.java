package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.EvaluationDto;
import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationResult;
import me.soels.tocairn.model.SolverConfiguration;
import me.soels.tocairn.services.EvaluationRunner;
import me.soels.tocairn.services.EvaluationService;
import me.soels.tocairn.services.SolutionService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Controller for managing the {@link Evaluation} and its {@link SolverConfiguration}.
 * <p>
 * This controller furthermore allows to run an evaluation.
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final EvaluationService service;
    private final SolutionService solutionService;
    private final EvaluationRunner runner;

    public EvaluationController(EvaluationService service, SolutionService solutionService, EvaluationRunner runner) {
        this.service = service;
        this.solutionService = solutionService;
        this.runner = runner;
    }

    /**
     * Retrieve all evaluations configuration in this application.
     *
     * @return all the evaluations configured
     */
    @GetMapping
    public List<EvaluationDto> getAllEvaluations() {
        return service.getShallowEvaluations().stream()
                .map(EvaluationDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the evaluation with the given {@code id}.
     *
     * @param evaluationId the id of the evaluation to retrieve
     * @return the retrieved evaluation
     */
    @GetMapping("/{evaluationId}")
    public EvaluationDto getEvaluation(@PathVariable UUID evaluationId) {
        return new EvaluationDto(service.getShallowEvaluation(evaluationId));
    }

    /**
     * Creates an evaluation.
     * <p>
     * Note that the objectives can not be changed afterwards.
     *
     * @param dto the DTO carrying the values of the evaluation to create
     * @return the created evaluation
     */
    @PostMapping
    @ResponseStatus(CREATED)
    public EvaluationDto createEvaluation(@Valid @RequestBody EvaluationDto dto) {
        return new EvaluationDto(service.createEvaluation(dto));
    }

    /**
     * Updates the evaluation object, overwriting all modifiable values with the given body.
     *
     * @param evaluationId the id of the evaluation to update
     * @param dto          the updated information.
     * @return the updated evaluation
     */
    @PutMapping("/{evaluationId}")
    public EvaluationDto updateEvaluation(@PathVariable UUID evaluationId, @Valid @RequestBody EvaluationDto dto) {
        return new EvaluationDto(service.updateEvaluation(evaluationId, dto));
    }

    /**
     * Deletes the evaluation with the provided ID.
     * <p>
     * Performs cascading deletes to the configuration, input graph and results as well.
     *
     * @param evaluationId the id of the evaluation to delete
     */
    @DeleteMapping("/{evaluationId}")
    @ResponseStatus(NO_CONTENT)
    public void deleteEvaluation(@PathVariable UUID evaluationId) {
        service.deleteEvaluation(evaluationId);
    }

    @DeleteMapping("/all")
    @ResponseStatus(NO_CONTENT)
    public void deleteAllEvaluations() {
        getAllEvaluations().stream().map(EvaluationDto::getId).forEach(service::deleteEvaluation);
    }

    /**
     * Runs the evaluation with the given {@code id}.
     * <p>
     * This run will be done asynchronously and the returned evaluation shows the state of the evaluation just after
     * starting this run.
     * <p>
     * This will start without a given solution. For AHCA, this means every class in its own cluster and for MOECA a
     * random clustering within the configured bounds.
     *
     * @param evaluationId the id of the evaluation to run
     * @param name         the name of the resulting {@link EvaluationResult}
     * @return the updated evaluation
     */
    @PostMapping("/{evaluationId}/run")
    public EvaluationDto runEvaluation(@PathVariable UUID evaluationId, @RequestParam String name) {
        var evaluation = service.prepareRun(evaluationId);
        runner.runEvaluation(evaluation, name, null, false);
        return new EvaluationDto(evaluation);
    }

    /**
     * Runs the evaluation with the given {@code evaluationId} based on a solution with the given {@code solutionId}.
     * <p>
     * This run will be done asynchronously and the returned evaluation shows the state of the evaluation just after
     * starting this run.
     * <p>
     * This runs AHCA from this solution merging clusters further and MOECA by starting the evolution from this initial
     * parent.
     *
     * @param evaluationId the id of the evaluation to run
     * @param solutionId   the solution to start from
     * @param name         the name of the resulting {@link EvaluationResult}
     * @param all          whether to initialise all solution in the MOECA with the given solution instead of only one
     *                     with the rest randomized
     * @return the updated evaluation
     */
    @PostMapping("/{evaluationId}/continue/{solutionId}")
    public EvaluationDto continueEvaluation(@PathVariable UUID evaluationId, @PathVariable UUID solutionId, @RequestParam String name, @RequestParam(required = false) Boolean all) {
        var evaluation = service.prepareRun(evaluationId);
        var solution = solutionService.getSolution(solutionId);
        runner.runEvaluation(evaluation, name, solution, Boolean.TRUE.equals(all));
        return new EvaluationDto(evaluation);
    }
}
