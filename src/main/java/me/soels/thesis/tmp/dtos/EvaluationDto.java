package me.soels.thesis.tmp.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import me.soels.thesis.tmp.daos.Evaluation;
import me.soels.thesis.tmp.daos.EvaluationResult;
import me.soels.thesis.tmp.daos.EvaluationStatus;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data transfer object for an evaluation.
 * <p>
 * An evaluation is the core data structure that manages the desired objectives, the model to run an analysis with
 * based off of the required sorts of analysis, metrics of the evaluation run and the solutions.
 */
public class EvaluationDto {
    private final UUID id;
    @NotBlank
    private final String name;
    private final EvaluationStatus status;
    @NotNull
    private final EvaluationConfigurationDto configuration;
    private final List<UUID> results;
    // TODO: Inputs

    @JsonCreator
    public EvaluationDto(@NotBlank String name, @Valid @NotNull EvaluationConfigurationDto configuration) {
        this.name = name;
        this.configuration = configuration;

        // Non-settable properties by user
        this.id = null;
        this.status = null;
        this.results = null;
    }

    public EvaluationDto(@NotNull Evaluation dao) {
        this.id = dao.getId();
        this.name = dao.getName();
        this.status = dao.getStatus();
        this.configuration = new EvaluationConfigurationDto(dao.getConfiguration());
        this.results = dao.getResults().stream()
                .map(EvaluationResult::getId)
                .collect(Collectors.toList());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public EvaluationConfigurationDto getConfiguration() {
        return configuration;
    }

    public List<UUID> getResults() {
        return results;
    }

    public Evaluation toDao() {
        var dao = new Evaluation();
        dao.setName(name);
        dao.setConfiguration(configuration.toDao());
        return dao;
    }
}
