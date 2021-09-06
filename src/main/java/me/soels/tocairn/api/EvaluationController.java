package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.EvaluationDto;
import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.SolverConfiguration;
import me.soels.tocairn.services.EvaluationRunner;
import me.soels.tocairn.services.EvaluationService;
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
    private final EvaluationRunner runner;

    public EvaluationController(EvaluationService service, EvaluationRunner runner) {
        this.service = service;
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

    /**
     * Runs the evaluation with the given {@code id}.
     * <p>
     * This run will be done asynchronously and the returned evaluation shows the state of the evaluation just after
     * starting this run.
     *
     * @param evaluationId the id of the evaluation to run
     * @return the updated evaluation
     */
    @PostMapping("/{evaluationId}/run")
    public EvaluationDto runEvaluation(@PathVariable UUID evaluationId) {
        var evaluation = service.prepareRun(evaluationId);
        runner.runEvaluation(evaluation);
        return new EvaluationDto(evaluation);
    }
}
