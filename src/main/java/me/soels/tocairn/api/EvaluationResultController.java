package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.EvaluationResultDto;
import me.soels.tocairn.model.EvaluationResult;
import me.soels.tocairn.services.EvaluationResultService;
import me.soels.tocairn.services.EvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Controller for listing {@link EvaluationResult}.
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
        return new EvaluationResultDto(resultService.getShallowResult(evaluationResultId));
    }

    @GetMapping("/evaluation/{evaluationId}/result")
    public List<EvaluationResultDto> getEvaluationResultsForEvaluation(@PathVariable UUID evaluationId) {
        var evaluation = evaluationService.getShallowEvaluation(evaluationId);
        return evaluation.getResults().stream()
                .map(result -> resultService.getShallowResult(result.getId()))
                .map(EvaluationResultDto::new)
                .collect(Collectors.toList());
    }

    @ResponseStatus(NO_CONTENT)
    @DeleteMapping("/result/{evaluationResultId}")
    public void deleteEvaluationResult(@PathVariable UUID evaluationResultId) {
        resultService.deleteResult(evaluationResultId);
    }
}
