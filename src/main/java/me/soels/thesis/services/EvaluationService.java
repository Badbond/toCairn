package me.soels.thesis.services;

import me.soels.thesis.api.ResourceNotFoundException;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationConfiguration;
import me.soels.thesis.model.EvaluationStatus;
import me.soels.thesis.api.dtos.EvaluationDto;
import me.soels.thesis.repositories.EvaluationConfigurationRepository;
import me.soels.thesis.repositories.EvaluationRepository;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing evaluations and their configuration.
 * <p>
 * Furthermore, this service allows the user to run the the evaluation after validation.
 */
@Service
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationConfigurationRepository configurationRepository;
    private final EvaluationRunner runner;
    private final EvaluationInputService inputService;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             EvaluationConfigurationRepository configurationRepository,
                             EvaluationRunner runner,
                             EvaluationInputService inputService) {
        this.evaluationRepository = evaluationRepository;
        this.configurationRepository = configurationRepository;
        this.runner = runner;
        this.inputService = inputService;
    }

    /**
     * Retrieve all stored evaluations.
     *
     * @return the stored evaluations
     */
    public List<Evaluation> getEvaluations() {
        return evaluationRepository.findAll();
    }

    /**
     * Retrieve a specific evaluation.
     *
     * @param id the id of the evaluation to retrieve
     * @return the requested evaluation
     */
    public Evaluation getEvaluation(UUID id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /**
     * Creates a new evaluation and its {@link EvaluationConfiguration}.
     *
     * @param dto the DTO to construct the evaluation from
     * @return the newly created evaluation and its configuration
     */
    public Evaluation createEvaluation(EvaluationDto dto) {
        var newEvaluation = dto.toDao();
        newEvaluation.setObjectives(dto.getObjectives());
        newEvaluation.setStatus(EvaluationStatus.INCOMPLETE);

        var configuration = validateConfiguration(newEvaluation.getConfiguration());
        newEvaluation.setConfiguration(configurationRepository.save(configuration));

        return evaluationRepository.save(newEvaluation);
    }

    /**
     * Updates the evaluation identified with {@code id} based on the information given in {@code newEvaluation}.
     * <p>
     * Only modifiable fields are updated.
     *
     * @param id  the evaluation to update
     * @param dto the DTO carrying the new values to update
     * @return the updated evaluation
     */
    public Evaluation updateEvaluation(UUID id, EvaluationDto dto) {
        var evaluation = getEvaluation(id);
        if (evaluation.getStatus() == EvaluationStatus.RUNNING) {
            throw new IllegalArgumentException("Can not change an evaluation that is running");
        }

        // TODO: Once all information is provided we need to (somewhere else) set the status to pending.
        var configuration = evaluation.getConfiguration();
        var newConfiguration = dto.getConfiguration().toDao();
        validateConfiguration(newConfiguration);
        newConfiguration.setId(configuration.getId());
        configurationRepository.save(newConfiguration);

        evaluation.setName(dto.getName());
        updateEvaluationStatus(evaluation);
        return evaluationRepository.save(evaluation);
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
        if (evaluation.getStatus() == EvaluationStatus.RUNNING) {
            throw new IllegalArgumentException("The evaluation is already running");
        }

        updateEvaluationStatus(evaluation);
        if (evaluation.getStatus() == EvaluationStatus.INCOMPLETE) {
            throw new IllegalArgumentException("Can not initiate run while not all inputs are provided");
        }

        evaluation.setStatus(EvaluationStatus.RUNNING);
        var result = evaluationRepository.save(evaluation);
        runner.runEvaluation(evaluation);
        return result;
    }

    public void updateEvaluationStatus(Evaluation evaluation) {
        var status = inputService.hasAllRequiredInput(evaluation.getId(), evaluation.getObjectives()) ?
                EvaluationStatus.PENDING :
                EvaluationStatus.INCOMPLETE;
        evaluation.setStatus(status);
    }

    /**
     * Validates the configuration.
     * <p>
     * Partial validation is done through javax validation as this was not done yet on controller-level.
     *
     * @param configuration the configuration to validate
     * @return the validated configuration
     */
    private EvaluationConfiguration validateConfiguration(@Valid EvaluationConfiguration configuration) {
        var boundViolation = configuration.getClusterCountLowerBound()
                .flatMap(lower -> configuration.getClusterCountUpperBound())
                .filter(upper -> upper < configuration.getClusterCountLowerBound().get());
        if (boundViolation.isPresent()) {
            throw new IllegalArgumentException("Cluster count upper bound needs to be greater than its lower bound");
        }
        return configuration;
    }
}
