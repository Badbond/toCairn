package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.CustomSolutionDto;
import me.soels.tocairn.api.dtos.SolutionDto;
import me.soels.tocairn.model.CustomEvaluationResult;
import me.soels.tocairn.model.Solution;
import me.soels.tocairn.services.EvaluationResultService;
import me.soels.tocairn.services.SolutionService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * Controller for listing {@link Solution}.
 */
@RestController
@RequestMapping("/api")
public class SolutionController {
    private final SolutionService solutionService;
    private final EvaluationResultService resultService;

    public SolutionController(SolutionService solutionService, EvaluationResultService resultService) {
        this.solutionService = solutionService;
        this.resultService = resultService;
    }

    @GetMapping("/solution/{solutionId}")
    public SolutionDto getSolution(@PathVariable UUID solutionId) {
        return new SolutionDto(solutionService.getShallowSolution(solutionId));
    }

    /**
     * Allows creating a solution based on the given solution and microservice structure.
     * <p>
     * Useful for creating a manual model for which to continue the clustering algorithm from or to calculate metrics
     * to manually investigate.
     * <p>
     * This will also create a {@link CustomEvaluationResult} node to adhere to database model standards. However,
     * this is not returned.
     *
     * @param customSolution the solution structure to create
     * @return the persisted solution
     */
    @PostMapping("/evaluation/{evaluationId}/solution")
    @ResponseStatus(CREATED)
    public SolutionDto createSolution(@PathVariable UUID evaluationId, @RequestBody CustomSolutionDto customSolution) {
        var solution = solutionService.createCustomSolution(evaluationId, customSolution);
        resultService.createAndPersist(evaluationId, solution);
        return new SolutionDto(solution);
    }

    /**
     * Calculates the metrics for the given solution and persists it.
     * <p>
     * Useful for operating on a manual model to investigate quality in terms of those metrics.
     *
     * @param evaluationId the evaluation ID to determine metrics from
     * @param solutionId   the solution ID to determine metrics for
     * @return the persisted solution
     */
    @PostMapping("/evaluation/{evaluationId}/solution/{solutionId}/calculateMetrics")
    public SolutionDto calculateMetrics(@PathVariable UUID evaluationId, @PathVariable UUID solutionId) {
        return new SolutionDto(solutionService.calculateMetrics(evaluationId, solutionId));
    }
}
