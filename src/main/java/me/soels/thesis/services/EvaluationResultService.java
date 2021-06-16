package me.soels.thesis.services;

import me.soels.thesis.api.ResourceNotFoundException;
import me.soels.thesis.model.Cluster;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.model.Solution;
import me.soels.thesis.repositories.ClusterRepository;
import me.soels.thesis.repositories.EvaluationRepository;
import me.soels.thesis.repositories.EvaluationResultRepository;
import me.soels.thesis.repositories.SolutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing {@link EvaluationResult}
 */
@Service
public class EvaluationResultService {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationResultRepository resultRepository;
    private final SolutionRepository solutionRepository;
    private final ClusterRepository clusterRepository;

    public EvaluationResultService(EvaluationRepository evaluationRepository,
                                   EvaluationResultRepository resultRepository,
                                   SolutionRepository solutionRepository,
                                   ClusterRepository clusterRepository) {
        this.evaluationRepository = evaluationRepository;
        this.resultRepository = resultRepository;
        this.solutionRepository = solutionRepository;
        this.clusterRepository = clusterRepository;
    }

    public void deleteResult(UUID id) {
        var maybeResult = resultRepository.findById(id);
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

    public EvaluationResult getResult(UUID id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Solution getSolution(UUID id) {
        return solutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public EvaluationResult storeResult(Evaluation evaluation, EvaluationResult result) {
        var storedResult = resultRepository.save(result);
        evaluation.getResults().add(storedResult);
        evaluationRepository.save(evaluation);
        return storedResult;
    }

    /**
     * Stores the given solutions and adds those solutions to the given (potentially yet unmanaged) result.
     *
     * @param result    the result to apply the solutions to
     * @param solutions the solutions to persist
     */
    public void storeSolutions(EvaluationResult result, List<Solution> solutions) {
        var storedSolution = solutionRepository.saveAll(solutions);
        result.getSolutions().addAll(storedSolution);
    }

    /**
     * Stores the given clusters and adds those clusters to the given (potentially yet unmanaged) solution.
     *
     * @param solution the solution to apply the created clusters to
     * @param clusters the clusters to persist
     */
    public void storeClusters(Solution solution, List<Cluster> clusters) {
        var storedClusters = clusterRepository.saveAll(clusters);
        solution.getClusters().addAll(storedClusters);
    }

    public List<EvaluationResult> getAllResults() {
        return resultRepository.findAll();
    }
}
