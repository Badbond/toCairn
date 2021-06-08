package me.soels.thesis.tmp.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.encoding.EncodingType;
import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import me.soels.thesis.tmp.daos.EvolutionaryAlgorithm;
import me.soels.thesis.tmp.daos.Objective;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
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

    @Size(min = 1)
    private final Integer clusterCountLowerBound;

    @Size(min = 1)
    private final Integer clusterCountUpperBound;

    @NotNull
    @Size(min = 2)
    private final List<Objective> objectives;

    public EvaluationConfigurationDto(EvaluationConfiguration dao) {
        this.algorithm = dao.getAlgorithm();
        this.encodingType = dao.getEncodingType();
        this.objectives = dao.getObjectives();
        this.clusterCountLowerBound = dao.getClusterCountLowerBound().orElse(null);
        this.clusterCountUpperBound = dao.getClusterCountUpperBound().orElse(null);
    }

    public EvaluationConfiguration toDao() {
        var dao = new EvaluationConfiguration();
        dao.setAlgorithm(algorithm);
        dao.setEncodingType(encodingType);
        dao.setObjectives(objectives);
        dao.setClusterCountLowerBound(clusterCountLowerBound);
        dao.setClusterCountUpperBound(clusterCountUpperBound);
        return dao;
    }

    public Optional<Integer> getClusterCountLowerBound() {
        return Optional.ofNullable(clusterCountLowerBound);
    }

    public Optional<Integer> getClusterCountUpperBound() {
        return Optional.ofNullable(clusterCountUpperBound);
    }
}
