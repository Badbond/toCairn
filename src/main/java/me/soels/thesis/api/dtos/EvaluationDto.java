package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.thesis.model.*;
import me.soels.thesis.solver.objectives.ObjectiveType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data transfer object for an evaluation.
 * <p>
 * An evaluation is the core data structure that manages the desired objectives, the model to run an analysis with
 * based off of the required sorts of analysis, metrics of the evaluation run and the solutions.
 * <p>
 * On create, one must provide the objectives desired to run. These can not be modified afterwards as part of the input
 * model might already be built which would require decomposing this model when an objective is removed. Furthermore,
 * when an evaluation ran, the results are based on the objectives used and the input model generated from those
 * input requirements and changing the objectives would invalidate this relationship.
 */
@Getter
public class EvaluationDto {
    private final UUID id;
    @NotBlank
    private final String name;
    @NotNull
    @Size(min = 1)
    private final Set<ObjectiveType> objectives;
    @NotNull
    private final SolverConfigurationDto solverConfiguration;
    private final EvaluationStatus status;
    private final Set<AnalysisType> executedAnalysis;
    private final List<UUID> results;

    @JsonCreator
    public EvaluationDto(String name, Set<ObjectiveType> objectives, SolverConfigurationDto solverConfiguration) {
        this.name = name;
        this.solverConfiguration = solverConfiguration;
        this.objectives = objectives;

        // Non-settable properties by user
        this.id = null;
        this.status = null;
        this.executedAnalysis = null;
        this.results = null;
    }

    public EvaluationDto(Evaluation dao) {
        this.id = dao.getId();
        this.name = dao.getName();
        this.objectives = dao.getObjectives();
        this.solverConfiguration = convertConfiguration(dao.getConfiguration());
        this.status = dao.getStatus();
        this.executedAnalysis = dao.getExecutedAnalysis();
        this.results = dao.getResults().stream()
                .map(EvaluationResult::getId)
                .collect(Collectors.toList());
    }

    private static SolverConfigurationDto convertConfiguration(SolverConfiguration configuration) {
        if (configuration instanceof MOEAConfiguration) {
            return new MOEAConfigurationDto((MOEAConfiguration) configuration);
        } else if (configuration instanceof HierarchicalConfiguration) {
            return new HierarchicalConfigurationDto((HierarchicalConfiguration) configuration);
        } else {
            throw new IllegalStateException("Unknown type of configuration found " + configuration.getClass().getSimpleName());
        }
    }

    public Evaluation toDao() {
        var dao = new Evaluation();
        dao.setName(name);
        dao.setConfiguration(solverConfiguration.toDao());
        // We don't set objectives here as only on create we should include them.
        return dao;
    }
}
