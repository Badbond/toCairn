package me.soels.thesis.services;

import me.soels.thesis.api.ResourceNotFoundException;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.repositories.EvaluationResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing the result of an evaluation run.
 */
@Service
public class EvaluationResultService {
    private final EvaluationResultRepository repository;

    public EvaluationResultService(EvaluationResultRepository repository) {
        this.repository = repository;
    }

    public List<EvaluationResult> getResults() {
        return repository.findAll();
    }

    public EvaluationResult getResult(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
