package me.soels.thesis.tmp.services;

import me.soels.thesis.tmp.ResourceNotFoundException;
import me.soels.thesis.tmp.daos.Evaluation;
import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import me.soels.thesis.tmp.daos.EvaluationStatus;
import me.soels.thesis.tmp.repositories.EvaluationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing and initiating runs of evaluations.
 */
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

    public Evaluation createEvaluation(Evaluation evaluation, EvaluationConfiguration configuration) {
        updateStatusNewEvaluation(evaluation);
        evaluation.setConfiguration(configuration);
        return repository.save(evaluation);
    }

    /**
     * Updates the evaluation identified with {@code id} based on the information given in {@code newEvaluation}.
     * <p>
     * Only modifiable fields are updated.
     *
     * @param id            the evaluation to update
     * @param newEvaluation the new values to update
     * @return the updated evaluation
     */
    public Evaluation updateEvaluation(UUID id, Evaluation newEvaluation) {
        var evaluation = getEvaluation(id);
        evaluation.setName(newEvaluation.getName());
        evaluation.setConfiguration(newEvaluation.getConfiguration());
        updateStatusNewEvaluation(evaluation);
        return repository.save(evaluation);
    }

    /**
     * Runs the evaluation for the given {@code id}.
     * <p>
     * The evaluation runs asynchronously.
     *
     * @param id the evaluation to run.
     * @return the updated evaluation after initiating a run
     */
    public Evaluation run(UUID id) {
        var evaluation = getEvaluation(id);
        // TODO: On successful process of all given information resulting in the required inputs, we need to toggle the status.
        if (evaluation.getStatus() == EvaluationStatus.INCOMPLETE) {
            throw new IllegalArgumentException("Can not initiate run while not all inputs are provided");
        }

        evaluation.setStatus(EvaluationStatus.RUNNING);
        var result = repository.save(evaluation);
        runner.runEvaluation(evaluation);
        return result;
    }

    private void updateStatusNewEvaluation(Evaluation evaluation) {
        var status = isEvaluationInputComplete(evaluation) ?
                EvaluationStatus.PENDING :
                EvaluationStatus.INCOMPLETE;
        evaluation.setStatus(status);
    }

    public boolean isEvaluationInputComplete(Evaluation evaluation) {
        // TODO: Check if all the desired inputs are present
        return false;
    }
}
