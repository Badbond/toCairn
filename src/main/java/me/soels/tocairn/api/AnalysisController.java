package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.DynamicAnalysisInputDto;
import me.soels.tocairn.api.dtos.SourceAnalysisInputDto;
import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.services.EvaluationInputService;
import me.soels.tocairn.services.EvaluationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.UUID;

import static me.soels.tocairn.model.AnalysisType.DYNAMIC;
import static me.soels.tocairn.model.AnalysisType.SOURCE;

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

    @Async
    @PostMapping("/source")
    public void performSourceAnalysis(@PathVariable UUID evaluationId,
                                      @RequestBody @Valid SourceAnalysisInputDto inputDto) throws IOException {
        var evaluation = evaluationService.getEvaluationDeep(evaluationId);
        inputService.performSourceAnalysis(evaluation, inputDto.toDao());
        evaluationService.updateAnalysisRan(evaluation, SOURCE);
    }

    @Async
    @PostMapping("/dynamic")
    public void performDynamicAnalysis(@PathVariable UUID evaluationId,
                                       @RequestBody @Valid DynamicAnalysisInputDto inputDto) {
        var evaluation = evaluationService.getEvaluationDeep(evaluationId);
        inputService.performDynamicAnalysis(evaluation, inputDto.toDao());
        evaluationService.updateAnalysisRan(evaluation, DYNAMIC);
    }
}
