package me.soels.thesis.services;

import me.soels.thesis.api.ResourceNotFoundException;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.model.Solution;
import me.soels.thesis.repositories.ClusterRepository;
import me.soels.thesis.repositories.EvaluationResultRepository;
import me.soels.thesis.repositories.SolutionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for managing {@link EvaluationResult}
 */
@Service
public class EvaluationResultService {
    private final EvaluationResultRepository resultRepository;
    private final SolutionRepository solutionRepository;
    private final ClusterRepository clusterRepository;

    public EvaluationResultService(EvaluationResultRepository resultRepository,
                                   SolutionRepository solutionRepository,
                                   ClusterRepository clusterRepository) {
        this.resultRepository = resultRepository;
        this.solutionRepository = solutionRepository;
        this.clusterRepository = clusterRepository;
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
        var maybeResult = resultRepository.getByIdShallow(id);
        // Delete the clusters associated with all the solutions in this result
        maybeResult.ifPresent(result -> result.getSolutions().stream()
                .flatMap(solution -> solution.getClusters().stream())
                .forEach(clusterRepository::delete));
        // Delete all the solutions
        maybeResult.ifPresent(result -> result.getSolutions()
                .forEach(solutionRepository::delete));
        // Delete the run result itself
        maybeResult.ifPresent(resultRepository::delete);
    }

    public EvaluationResult getShallowResult(UUID id) {
        return resultRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Solution getShallowSolution(UUID id) {
        return solutionRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
