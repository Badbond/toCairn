package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.tocairn.model.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
    private final SolverConfigurationDto solverConfiguration;
    private final EvaluationStatus status;
    private final Set<AnalysisType> executedAnalysis;
    private final List<UUID> results;

    @JsonCreator
    public EvaluationDto(String name, SolverConfigurationDto solverConfiguration) {
        this.name = name;
        this.solverConfiguration = solverConfiguration;

        // Non-settable properties by user
        this.id = null;
        this.status = null;
        this.executedAnalysis = null;
        this.results = null;
    }

    public EvaluationDto(Evaluation dao) {
        this.id = dao.getId();
        this.name = dao.getName();
        this.solverConfiguration = convertConfiguration(dao.getConfiguration());
        this.status = dao.getStatus();
        this.executedAnalysis = dao.getExecutedAnalysis();
        this.results = dao.getResults().stream()
                .map(EvaluationResult::getId)
                .collect(Collectors.toList());
    }

    private static SolverConfigurationDto convertConfiguration(SolverConfiguration configuration) {
        if (configuration instanceof MOECAConfiguration) {
            return new MOECAConfigurationDto((MOECAConfiguration) configuration);
        } else if (configuration instanceof AHCAConfiguration) {
            return new AHCAConfigurationDto((AHCAConfiguration) configuration);
        } else {
            throw new IllegalStateException("Unknown type of configuration found " + configuration.getClass().getSimpleName());
        }
    }

    public Evaluation toDao() {
        var dao = new Evaluation();
        dao.setName(name);
        dao.setConfiguration(solverConfiguration.toDao());
        dao.setStatus(status);
        return dao;
    }
}
