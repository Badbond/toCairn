package me.soels.thesis.tmp.services;

import me.soels.thesis.tmp.ResourceNotFoundException;
import me.soels.thesis.tmp.repositories.EvaluationRepository;
import me.soels.thesis.tmp.daos.Evaluation;
import me.soels.thesis.tmp.daos.EvaluationStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EvaluationService {
    private final EvaluationRepository repository;
    private final EvaluationRunner runner;

    public EvaluationService(EvaluationRepository repository, EvaluationRunner runner) {
        this.repository = repository;
        this.runner = runner;
    }

    public List<Evaluation> getEvaluations() {
        return repository.findAll();
    }

    public Evaluation getEvaluation(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Evaluation createEvaluation(Evaluation evaluation) {
        updateStatusNewEvaluation(evaluation);
        return repository.save(evaluation);
    }

    public Evaluation updateEvaluation(UUID id, Evaluation newEvaluation) {
        var evaluation = getEvaluation(id);
        evaluation.setName(newEvaluation.getName());
        evaluation.setConfiguration(newEvaluation.getConfiguration());
        updateStatusNewEvaluation(evaluation);
        return repository.save(evaluation);
    }

    public Evaluation run(UUID id) {
        var evaluation = getEvaluation(id);
        runner.runEvaluation(evaluation);
        evaluation.setStatus(EvaluationStatus.RUNNING);
        return repository.save(evaluation);
    }

    private void updateStatusNewEvaluation(Evaluation evaluation) {
        var status = isEvaluationComplete(evaluation) ?
                EvaluationStatus.PENDING :
                EvaluationStatus.INCOMPLETE;
        evaluation.setStatus(status);
    }

    public boolean isEvaluationComplete(Evaluation evaluation) {
        // TODO: Check if all the desired inputs are present
        return false;
    }
}
