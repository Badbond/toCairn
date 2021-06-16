package me.soels.thesis.api;

import me.soels.thesis.api.dtos.EvaluationResultDto;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.services.EvaluationResultService;
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
    private final EvaluationResultService resultService;
    private final EvaluationService evaluationService;

    public EvaluationResultController(EvaluationResultService resultService, EvaluationService evaluationService) {
        this.resultService = resultService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/result/{evaluationResultId}")
    public EvaluationResultDto getEvaluationResultById(@PathVariable UUID evaluationResultId) {
        return new EvaluationResultDto(resultService.getResult(evaluationResultId));
    }

    @GetMapping("/evaluation/{evaluationId}/result")
    public List<EvaluationResultDto> getEvaluationResultsForEvaluation(@PathVariable UUID evaluationId) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        return evaluation.getResults().stream()
                .map(EvaluationResultDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/solution/{solutionId}")
    public SolutionDto getSolution(@PathVariable UUID solutionId) {
        return new SolutionDto(resultService.getSolution(solutionId));
    }

    @ResponseStatus(NO_CONTENT)
    @DeleteMapping("/result/{evaluationResultId}")
    public void deleteEvaluationResult(@PathVariable UUID evaluationResultId) {
        resultService.deleteResult(evaluationResultId);
    }
}
