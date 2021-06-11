package me.soels.thesis.api;

import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.services.EvaluationInputService;
import me.soels.thesis.services.EvaluationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/static")
    public void performStaticAnalysis(@PathVariable UUID evaluationId /*, TODO: body*/) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        // TODO: Extract input configuration from body
        var staticAnalysisInput = new StaticAnalysisInput(null, null, null);
        inputService.performStaticAnalysis(evaluation, staticAnalysisInput);
        evaluationService.updateAnalysisRan(evaluation, STATIC);
    }

    @Async
    @PostMapping("/dynamic")
    public void performDynamicAnalysis(@PathVariable UUID evaluationId /*, TODO: body*/) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        // TODO: Extract input configuration from body
        var dynamicAnalysisInput = new DynamicAnalysisInput(null);
        inputService.performDynamicAnalysis(evaluation, dynamicAnalysisInput);
        evaluationService.updateAnalysisRan(evaluation, DYNAMIC);
    }

    @Async
    @PostMapping("/evolutionary")
    public void performEvolutionaryAnalysis(@PathVariable UUID evaluationId /*, TODO: body*/) {
        var evaluation = evaluationService.getEvaluation(evaluationId);
        // TODO: Extract input configuration from body
        var evolutionaryAnalysisInput = new EvolutionaryAnalysisInput(null);
        inputService.performEvolutionaryAnalysis(evaluation, evolutionaryAnalysisInput);
        evaluationService.updateAnalysisRan(evaluation, EVOLUTIONARY);
    }
}
