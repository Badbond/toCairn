package me.soels.thesis.api;

import me.soels.thesis.api.dtos.EvaluationResultDto;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.repositories.EvaluationResultRepository;
import me.soels.thesis.services.EvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Controller for listing {@link EvaluationResult}.
 * <p>
 * TODO: How to expose the solutions in the clustering from optimization?
 */
@RestController
@RequestMapping("/api")
public class EvaluationResultController {
    private final EvaluationResultRepository repository;
    private final EvaluationService evaluationService;

    public EvaluationResultController(EvaluationResultRepository repository, EvaluationService evaluationService) {
        this.repository = repository;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/result")
    public List<EvaluationResultDto> getAllEvaluationResults() {
        return repository.findAll().stream()
                .map(EvaluationResultDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/result/{evaluationResultId}}")
    public EvaluationResultDto getEvaluationResultById(@PathVariable UUID evaluationResultId) {
        return new EvaluationResultDto(getResult(evaluationResultId));
    }

    @GetMapping("/evaluation/{evaluationId}/result")
    public List<EvaluationResultDto> getEvaluationResultsForEvaluation(@PathVariable UUID evaluationId) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        return evaluation.getResults().stream()
                .map(EvaluationResultDto::new)
                .collect(Collectors.toList());
    }

    @ResponseStatus(NO_CONTENT)
    @DeleteMapping("/result/{evaluationResultId}")
    public void deleteEvaluationResult(@PathVariable UUID evaluationResultId) {
        repository.deleteById(evaluationResultId);
    }

    private EvaluationResult getResult(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
