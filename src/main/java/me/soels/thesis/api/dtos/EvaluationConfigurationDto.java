package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.solver.moea.encoding.EncodingType;
import me.soels.thesis.model.SolverConfiguration;
import me.soels.thesis.model.EvolutionaryAlgorithm;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Optional;

/**
 * Models the configuration of an evaluation.
 */
@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class EvaluationConfigurationDto {
    @NotNull
    private final EvolutionaryAlgorithm algorithm;

    @NotNull
    private final EncodingType encodingType;

    @NotNull
    @Size(min = 10)
    private final int maxEvaluations;

    @Size(min = 1000)
    private final Long maxTime;

    @Size(min = 1)
    private final Integer clusterCountLowerBound;

    @Size(min = 1)
    private final Integer clusterCountUpperBound;

    public EvaluationConfigurationDto(SolverConfiguration dao) {
        this.algorithm = dao.getAlgorithm();
        this.encodingType = dao.getEncodingType();
        this.maxEvaluations = dao.getMaxEvaluations();
        this.maxTime = dao.getMaxTime().orElse(null);
        this.clusterCountLowerBound = dao.getClusterCountLowerBound().orElse(null);
        this.clusterCountUpperBound = dao.getClusterCountUpperBound().orElse(null);
    }

    public SolverConfiguration toDao() {
        var dao = new SolverConfiguration();
        dao.setAlgorithm(algorithm);
        dao.setEncodingType(encodingType);
        dao.setMaxEvaluations(maxEvaluations);
        dao.setMaxTime(maxTime);
        dao.setClusterCountLowerBound(clusterCountLowerBound);
        dao.setClusterCountUpperBound(clusterCountUpperBound);
        return dao;
    }

    public Optional<Long> getMaxTime() {
        return Optional.ofNullable(maxTime);
    }

    public Optional<Integer> getClusterCountLowerBound() {
        return Optional.ofNullable(clusterCountLowerBound);
    }

    public Optional<Integer> getClusterCountUpperBound() {
        return Optional.ofNullable(clusterCountUpperBound);
    }
}
