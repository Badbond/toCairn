package me.soels.tocairn.services;

import me.soels.tocairn.api.ResourceNotFoundException;
import me.soels.tocairn.model.CustomEvaluationResult;
import me.soels.tocairn.model.EvaluationResult;
import me.soels.tocairn.model.Solution;
import me.soels.tocairn.repositories.EvaluationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for managing {@link EvaluationResult}
 */
@Service
public class EvaluationResultService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationResultService.class);
    private final EvaluationResultRepository resultRepository;
    private final SolutionService solutionService;
    private final EvaluationService evaluationService;

    public EvaluationResultService(EvaluationResultRepository resultRepository,
                                   SolutionService solutionService,
                                   EvaluationService evaluationService) {
        this.resultRepository = resultRepository;
        this.solutionService = solutionService;
        this.evaluationService = evaluationService;
    }

    /**
     * Returns the result and its relationships to solutions.
     * <p>
     * This does not return the solution's microservices.
     *
     * @param id the ID of the result to retrieve
     * @return the shallow result object
     */
    public EvaluationResult getShallowResult(UUID id) {
        return resultRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /**
     * Delete the result with the given {@code id}.
     * <p>
     * Performs cascading delete for the solutions in this results and the clusters in the solutions. The input
     * graph is not deleted.
     *
     * @param id the id of the result to delete
     */
    public void deleteResult(UUID id) {
        var maybeResult = resultRepository.getByIdShallowWithMicroservices(id);
        // Delete the solutions and their microservices
        maybeResult.ifPresent(result -> result.getSolutions().forEach(solutionService::deleteSolution));
        // Delete the run result itself
        maybeResult.ifPresent(resultRepository::delete);
    }

    /**
     * Persists the given result and returns its persisted state with solution and microservice relationships.
     * <p>
     * We need to remove the solution relationships in the result to perform the persistence queries. We then add them
     * before returning again. Be wary of persisting this object in any other service/repository.
     *
     * @param result the result to persist
     */
    public void persistResult(EvaluationResult result) {
        LOGGER.info("Persisting clustering result containing {} solutions", result.getSolutions().size());
        LOGGER.info(" - Temporarily storing result's solutions and resetting them");
        var solutions = result.getSolutions().stream()
                .map(solutionService::persistSolution)
                .collect(Collectors.toList());
        result.setSolutions(Collections.emptyList());

        LOGGER.info(" - Persisting result.");
        resultRepository.save(result);

        LOGGER.info(" - Creating links from result to solutions");
        resultRepository.createSolutionRelationship(result.getId(), solutions.stream()
                .map(Solution::getId)
                .collect(Collectors.toList())
        );

        result.setSolutions(solutions);
        LOGGER.info("Done persisting clustering result(s)");
    }

    /**
     * Creates an {@link EvaluationResult} object to link the given {@code evaluationId} to the given {@code solution}
     * and persist the result and underlying data objects.
     *
     * @param evaluationId the evaluation to link the result to
     * @param solution     the solution to link the result to
     */
    public void createAndPersist(UUID evaluationId, Solution solution) {
        var result = new CustomEvaluationResult();
        result.setSolutions(List.of(solution));
        result.setName("Evaluation result for custom solution");
        var now = ZonedDateTime.now();
        result.setStartDate(now);
        result.setFinishDate(now);
        persistResult(result);
        evaluationService.createResultRelationship(evaluationId, result.getId());
    }
}
