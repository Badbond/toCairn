package me.soels.thesis.api;

import me.soels.thesis.api.dtos.EvolutionaryAnalysisInputDto;
import me.soels.thesis.api.dtos.SourceAnalysisInputDto;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.services.EvaluationInputService;
import me.soels.thesis.services.EvaluationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
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

    @Async
    @PostMapping("/source")
    public void performSourceAnalysis(@PathVariable UUID evaluationId,
                                      @RequestBody @Valid SourceAnalysisInputDto inputDto) throws IOException {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        inputService.performSourceAnalysis(evaluation, inputDto.toDao());
        evaluationService.updateAnalysisRan(evaluation, SOURCE);
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
