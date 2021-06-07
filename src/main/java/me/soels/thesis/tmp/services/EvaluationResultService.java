package me.soels.thesis.tmp.services;

import me.soels.thesis.tmp.ResourceNotFoundException;
import me.soels.thesis.tmp.daos.EvaluationResult;
import me.soels.thesis.tmp.repositories.EvaluationResultRepository;
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
