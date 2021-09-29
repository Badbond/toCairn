package me.soels.tocairn.services;

import me.soels.tocairn.api.ResourceNotFoundException;
import me.soels.tocairn.api.dtos.EvaluationDto;
import me.soels.tocairn.model.AnalysisType;
import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationStatus;
import me.soels.tocairn.model.SolverConfiguration;
import me.soels.tocairn.repositories.EvaluationRepository;
import me.soels.tocairn.repositories.SolverConfigurationRepository;
import me.soels.tocairn.solver.metric.MetricType;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static me.soels.tocairn.model.AnalysisType.*;
import static me.soels.tocairn.model.EvaluationStatus.INCOMPLETE;
import static me.soels.tocairn.model.EvaluationStatus.RUNNING;
import static me.soels.tocairn.solver.metric.MetricType.*;

/**
 * Service responsible for managing evaluations and their configuration including preparation steps for running
 * the evaluation and managing the status based on the provided inputs.
 */
@Service
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final SolverConfigurationRepository configurationRepository;
    private final EvaluationInputService inputService;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             SolverConfigurationRepository configurationRepository,
                             EvaluationInputService inputService) {
        this.evaluationRepository = evaluationRepository;
        this.configurationRepository = configurationRepository;
        this.inputService = inputService;
    }

    /**
     * Retrieve all stored evaluations without the input graph data.
     *
     * @return the stored shallow evaluations
     */
    public List<Evaluation> getShallowEvaluations() {
        return evaluationRepository.findAllShallow();
    }

    /**
     * Retrieve a specific evaluation.
     *
     * @param id the id of the evaluation to retrieve
     * @return the requested evaluation
     */
    public Evaluation getEvaluationDeep(UUID id) {
        var result = evaluationRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        inputService.populateInputFromDb(result);
        return result;
    }

    /**
     * Retrieve a specific evaluation without input graph data.
     *
     * @param id the id of the evaluation to retrieve
     * @return the requested shallow evaluation
     */
    public Evaluation getShallowEvaluation(UUID id) {
        return evaluationRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /**
     * Creates a new evaluation and its {@link SolverConfiguration}.
     *
     * @param dto the DTO to construct the evaluation from
     * @return the newly created evaluation and its configuration
     */
    public Evaluation createEvaluation(EvaluationDto dto) {
        var newEvaluation = dto.toDao();
        newEvaluation.setStatus(INCOMPLETE);

        var configuration = validateConfiguration(newEvaluation.getConfiguration());
        newEvaluation.setConfiguration(configurationRepository.save(configuration));

        return evaluationRepository.save(newEvaluation);
    }

    /**
     * Updates the evaluation identified with {@code id} based on the information given in the provided {@code dto}.
     * <p>
     * Only modifiable fields are updated.
     *
     * @param id  the evaluation to update
     * @param dto the DTO carrying the new values to update
     * @return the updated evaluation
     */
    public Evaluation updateEvaluation(UUID id, EvaluationDto dto) {
        var evaluation = getShallowEvaluation(id);
        if (evaluation.getStatus() == RUNNING) {
            throw new IllegalArgumentException("Can not change an evaluation that is running");
        }

        var configuration = evaluation.getConfiguration();
        var newConfiguration = dto.getSolverConfiguration().toDao();
        validateConfiguration(newConfiguration);
        newConfiguration.setId(configuration.getId());
        newConfiguration = configurationRepository.save(newConfiguration);
        evaluation.setConfiguration(newConfiguration);
        evaluation.setName(dto.getName());

        checkRanAnalysesAndUpdateStatus(evaluation);
        return evaluationRepository.saveShallow(evaluation);
    }

    public void createResultRelationship(UUID evaluationId, UUID resultId) {
        evaluationRepository.createResultRelationship(evaluationId, resultId);
    }

    /**
     * Deletes the evaluation with the provided {@code id}.
     * <p>
     * Performs cascading deletes on the configuration, input graph and results.
     *
     * @param id the evaluation to delete
     */
    public void deleteEvaluation(UUID id) {
        evaluationRepository.cascadeDelete(id);
    }

    /**
     * Prepares the run of an {@link Evaluation} for the given {@code id}.
     *
     * @param id the evaluation to prepare to run.
     * @return the updated evaluation after preparation
     */
    public Evaluation prepareRun(UUID id) {
        var evaluation = getEvaluationDeep(id);
        if (evaluation.getStatus() == RUNNING) {
            throw new IllegalArgumentException("The evaluation is already running");
        }

        checkRanAnalysesAndUpdateStatus(evaluation);
        if (evaluation.getStatus() == INCOMPLETE) {
            throw new IllegalArgumentException("Can not initiate run while not all inputs are provided");
        }

        evaluation.setStatus(RUNNING);
        evaluationRepository.saveShallow(evaluation);
        return evaluation;
    }

    /**
     * Updates and persists the status of the given evaluation based on the given {@code status}.
     *
     * @param evaluationId the evaluation to update
     * @param status       the status to persist
     */
    public void updateStatus(UUID evaluationId, EvaluationStatus status) {
        var evaluation = getShallowEvaluation(evaluationId);
        evaluation.setStatus(status);
        evaluationRepository.saveShallow(evaluation);
    }

    /**
     * Updates the analysis ran on the provided evaluation by adding the given analysis type.
     * This furthermore checks if all required analysis have ran based on the configured objectives and updates the
     * status of the evaluation accordingly. These changes to the evaluation are persisted.
     *
     * @param evaluation the evaluation to update
     * @param analysis   the analysis type to add
     */
    public void updateAnalysisRan(Evaluation evaluation, AnalysisType analysis) {
        evaluation.getExecutedAnalysis().add(analysis);
        checkRanAnalysesAndUpdateStatus(evaluation);
        evaluationRepository.saveShallow(evaluation);
    }

    /**
     * Checks which analyses have been performed and are required for the running the evaluation. Then updates the
     * evaluation's status to represent this accordingly.
     * <p>
     * This does not persist the evaluation.
     *
     * @param evaluation the evaluation to check inputs and update the status for
     */
    public void checkRanAnalysesAndUpdateStatus(Evaluation evaluation) {
        var newStatus = hasAllRequiredInput(evaluation, evaluation.getConfiguration().getMetrics()) ?
                EvaluationStatus.PENDING :
                INCOMPLETE;
        evaluation.setStatus(newStatus);
    }

    /**
     * Checks whether all the input is present for the given objectives.
     * <p>
     * Note that {@link AnalysisType#SOURCE} analysis is always required as this generates a complete graph of classes
     * to cluster.
     *
     * @param evaluation the evaluation to check whether all input has been provided
     * @param objectives the objectives to meet input for
     * @return whether all input has been provided given the objectives
     */
    public boolean hasAllRequiredInput(Evaluation evaluation, List<MetricType> objectives) {
        var executed = evaluation.getExecutedAnalysis();
        return executed.contains(SOURCE) &&
                (!objectives.contains(SHARED_DEVELOPMENT_LIFECYCLE) || executed.contains(EVOLUTIONARY)) &&
                (!objectives.contains(LIMITED_COMMUNICATION_OVERHEAD) || executed.contains(DYNAMIC)) &&
                (!objectives.contains(REUSABLE) || executed.contains(DYNAMIC)) &&
                (!objectives.contains(SEMANTIC_COUPLING) || executed.contains(SEMANTIC));
    }

    /**
     * Validates the evaluation's configuration.
     * <p>
     * Partial validation is done through javax validation as this was not done yet on controller-level.
     *
     * @param configuration the configuration to validate
     * @return the validated configuration
     */
    private SolverConfiguration validateConfiguration(@Valid SolverConfiguration configuration) {
        var boundViolation = configuration.getMinClusterAmount()
                .flatMap(min -> configuration.getMaxClusterAmount())
                .filter(max -> configuration.getMinClusterAmount().get() > max);
        if (boundViolation.isPresent()) {
            throw new IllegalArgumentException("maxClusterAmount needs to be greater than minClusterAmount");
        }
        return configuration;
    }
}
