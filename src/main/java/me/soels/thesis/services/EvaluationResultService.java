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

    public EvaluationResult storeResult(Evaluation evaluation, EvaluationResult result) {
        var storedResult = resultRepository.save(result);
        evaluation.getResults().add(storedResult);
        evaluationRepository.save(evaluation);
        return storedResult;
    }

    public Solution storeSolution(EvaluationResult result, Solution solution) {
        var storedSolution = solutionRepository.save(solution);
        result.getSolutions().add(storedSolution);
        resultRepository.save(result);
        return storedSolution;
    }

    public Cluster storeCluster(Solution solution, Cluster cluster) {
        var storedCluster = clusterRepository.save(cluster);
        solution.getClusters().add(storedCluster);
        solutionRepository.save(solution);
        return storedCluster;
    }

    public List<EvaluationResult> getAllResults() {
        return resultRepository.findAll();
    }
}
