package me.soels.thesis.tmp;

import me.soels.thesis.tmp.daos.Evaluation;
import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import me.soels.thesis.tmp.dtos.EvaluationDto;
import me.soels.thesis.tmp.services.EvaluationService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing the {@link Evaluation} and its {@link EvaluationConfiguration}.
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final EvaluationService service;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    /**
     * Retrieve all evaluations configuration in this application.
     *
     * @return all the evaluations configured
     */
    @GetMapping
    public List<EvaluationDto> getAllEvaluations() {
        return service.getEvaluations().stream()
                .map(EvaluationDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the evaluation with the given {@code id}.
     * <p>
     * Throws a 404 when the evaluation does not exist.
     *
     * @param id the id of the evaluation to retrieve
     * @return the retrieved evaluation
     */
    @GetMapping("/{id}")
    public EvaluationDto getEvaluation(@PathVariable UUID id) {
        return new EvaluationDto(service.getEvaluation(id));
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
    public EvaluationDto createEvaluation(@Valid @RequestBody EvaluationDto dto) {
        return new EvaluationDto(service.createEvaluation(dto));
    }

    /**
     * Runs the evaluation with the given {@code id}.
     * <p>
     * This run will be done asynchronously and the returned evaluation shows the state of the evaluation just after
     * starting this run.
     * <p>
     * Throws a 404 when the evaluation does not exist.
     *
     * @param id the id of the evaluation to run
     * @return the updated evaluation
     */
    @PostMapping("/{id}/run")
    public EvaluationDto runEvaluation(@PathVariable UUID id) {
        return new EvaluationDto(service.run(id));
    }

    /**
     * Updates the evaluation object, overwriting all modifiable values with the given body.
     * <p>
     * Throws a 404 when the evaluation does not exist.
     *
     * @param id  the id of the evaluation to update
     * @param dto the updated information.
     * @return the updated evaluation
     */
    @PutMapping("/{id}")
    public EvaluationDto updateEvaluation(@PathVariable UUID id, @Valid @RequestBody EvaluationDto dto) {
        return new EvaluationDto(service.updateEvaluation(id, dto));
    }

    // TODO: Delete
}