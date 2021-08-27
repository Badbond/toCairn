package me.soels.thesis.services;

import me.soels.thesis.api.ResourceNotFoundException;
import me.soels.thesis.api.dtos.EvaluationDto;
import me.soels.thesis.model.*;
import me.soels.thesis.repositories.EvaluationConfigurationRepository;
import me.soels.thesis.repositories.EvaluationRepository;
import me.soels.thesis.solver.objectives.ObjectiveType;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.soels.thesis.model.AnalysisType.EVOLUTIONARY;
import static me.soels.thesis.model.AnalysisType.SOURCE;
import static me.soels.thesis.model.EvaluationStatus.INCOMPLETE;
import static me.soels.thesis.model.EvaluationStatus.RUNNING;
import static me.soels.thesis.solver.objectives.ObjectiveType.SHARED_DEVELOPMENT_LIFECYCLE;

/**
 * Service responsible for managing evaluations and their configuration including preparation steps for running
 * the evaluation and managing the status based on the provided inputs.
 */
@Service
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationConfigurationRepository configurationRepository;
    private final EvaluationInputService inputService;
    private final EvaluationResultService resultService;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             EvaluationConfigurationRepository configurationRepository,
                             EvaluationInputService inputService,
                             EvaluationResultService resultService) {
        this.evaluationRepository = evaluationRepository;
        this.configurationRepository = configurationRepository;
        this.inputService = inputService;
        this.resultService = resultService;
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
    public Evaluation getEvaluation(UUID id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
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
        newEvaluation.setObjectives(dto.getObjectives());
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
        var evaluation = getEvaluation(id);
        if (evaluation.getStatus() == RUNNING) {
            throw new IllegalArgumentException("Can not change an evaluation that is running");
        }

        var configuration = evaluation.getConfiguration();
        var newConfiguration = dto.getConfiguration().toDao();
        validateConfiguration(newConfiguration);
        newConfiguration.setId(configuration.getId());
        configurationRepository.save(newConfiguration);

        evaluation.setName(dto.getName());
        checkRanAnalysesAndUpdateStatus(evaluation);
        return evaluationRepository.save(evaluation);
    }

    public Evaluation save(Evaluation evaluation) {
        return evaluationRepository.save(evaluation);
    }

    /**
     * Deletes the evaluation with the provided {@code id}.
     * <p>
     * Performs cascading deletes on the configuration, input graph and results.
     *
     * @param id the evaluation to delete
     */
    public void deleteEvaluation(UUID id) {
        evaluationRepository.findById(id).ifPresent(evaluation -> {
            evaluation.getResults().stream().map(EvaluationResult::getId).forEach(resultService::deleteResult);
            inputService.deleteAllInputs(evaluation);
            configurationRepository.deleteById(evaluation.getConfiguration().getId());
            evaluationRepository.deleteById(evaluation.getId());
        });
    }

    /**
     * Prepares the run of an {@link Evaluation} for the given {@code id}.
     *
     * @param id the evaluation to prepare to run.
     * @return the updated evaluation after preparation
     */
    public Evaluation prepareRun(UUID id) {
        var evaluation = getEvaluation(id);
        if (evaluation.getStatus() == RUNNING) {
            throw new IllegalArgumentException("The evaluation is already running");
        }

        checkRanAnalysesAndUpdateStatus(evaluation);
        if (evaluation.getStatus() == INCOMPLETE) {
            throw new IllegalArgumentException("Can not initiate run while not all inputs are provided");
        }

        evaluation.setStatus(RUNNING);
        return evaluationRepository.save(evaluation);
    }

    /**
     * Updates and persists the status of the given evaluation based on the given {@code status}.
     *
     * @param evaluation the evaluation to update
     * @param status     the status to persist
     */
    public void updateStatus(Evaluation evaluation, EvaluationStatus status) {
        evaluation.setStatus(status);
        evaluationRepository.save(evaluation);
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
        evaluationRepository.save(evaluation);
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
        var newStatus = hasAllRequiredInput(evaluation, evaluation.getObjectives()) ?
                EvaluationStatus.PENDING :
                INCOMPLETE;
        evaluation.setStatus(newStatus);
    }

    /**
     * Checks whether all the input is present for the given objectives.
     * <p>
     * Note that {@link AnalysisType#SOURCE} analysis is always required as this generates a complete graph of classes
     * to cluster. As {@link ObjectiveType#ONE_PURPOSE} and {@link ObjectiveType#BOUNDED_CONTEXT} only rely on the
     * result from source analysis, we do not check them explicitly.
     *
     * @param evaluation the evaluation to check whether all input has been provided
     * @param objectives the objectives to meet input for
     * @return whether all input has been provided given the objectives
     */
    public boolean hasAllRequiredInput(Evaluation evaluation, Set<ObjectiveType> objectives) {
        var executed = evaluation.getExecutedAnalysis();
        return executed.contains(SOURCE) &&
                (!objectives.contains(SHARED_DEVELOPMENT_LIFECYCLE) || executed.contains(EVOLUTIONARY));
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
        if (configuration instanceof MOEAConfiguration) {
            var moeaConfiguration = (MOEAConfiguration) configuration;
            var boundViolation = moeaConfiguration.getClusterCountLowerBound()
                    .flatMap(lower -> moeaConfiguration.getClusterCountUpperBound())
                    .filter(upper -> upper < moeaConfiguration.getClusterCountLowerBound().get());
            if (boundViolation.isPresent()) {
                throw new IllegalArgumentException("Cluster count upper bound needs to be greater than its lower bound");
            }
        }
        return configuration;
    }
}
