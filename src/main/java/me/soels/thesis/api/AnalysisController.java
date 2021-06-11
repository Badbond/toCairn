package me.soels.thesis.api;

import me.soels.thesis.api.dtos.DynamicAnalysisInputDto;
import me.soels.thesis.api.dtos.EvolutionaryAnalysisInputDto;
import me.soels.thesis.api.dtos.StaticAnalysisInputDto;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.services.EvaluationInputService;
import me.soels.thesis.services.EvaluationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

import static me.soels.thesis.model.AnalysisType.*;

/**
 * Controller containing endpoints to perform analysis which enhance the {@link EvaluationInput} for an
 * {@link Evaluation}.
 * <p>
 * Note that all analyses are performed asynchronously. Therefore, this controller returns immediately and the results
 * are only visible after analysis has been completed.
 */
@RestController
@RequestMapping("/api/evaluation/{evaluationId}/analysis")
public class AnalysisController {
    private final EvaluationService evaluationService;
    private final EvaluationInputService inputService;

    public AnalysisController(EvaluationService evaluationService, EvaluationInputService inputService) {
        this.evaluationService = evaluationService;
        this.inputService = inputService;
    }

    @Async // TODO: This does not work... (same goes for other 2)
    @PostMapping("/static")
    public void performStaticAnalysis(@PathVariable UUID evaluationId,
                                      @RequestBody @Valid StaticAnalysisInputDto inputDto) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        inputService.performStaticAnalysis(evaluation, inputDto.toDao());
        evaluationService.updateAnalysisRan(evaluation, STATIC);
    }

    @Async
    @PostMapping("/dynamic")
    public void performDynamicAnalysis(@PathVariable UUID evaluationId,
                                       @RequestBody @Valid DynamicAnalysisInputDto inputDto) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        inputService.performDynamicAnalysis(evaluation, inputDto.toDao());
        evaluationService.updateAnalysisRan(evaluation, DYNAMIC);
    }

    @Async
    @PostMapping("/evolutionary")
    public void performEvolutionaryAnalysis(@PathVariable UUID evaluationId,
                                            @RequestBody @Valid EvolutionaryAnalysisInputDto inputDto) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        inputService.performEvolutionaryAnalysis(evaluation, inputDto.toDao());
        evaluationService.updateAnalysisRan(evaluation, EVOLUTIONARY);
    }
}
