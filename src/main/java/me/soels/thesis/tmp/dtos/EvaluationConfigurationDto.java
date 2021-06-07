package me.soels.thesis.tmp.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import me.soels.thesis.tmp.daos.EvolutionaryAlgorithm;
import me.soels.thesis.tmp.daos.Objective;
import me.soels.thesis.encoding.EncodingType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Optional;

/**
 * Models the configuration of an evaluation.
 */
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

    @JsonCreator
    public EvaluationConfigurationDto(@NotNull EvolutionaryAlgorithm algorithm,
                                      @NotNull EncodingType encodingType,
                                      @NotNull List<Objective> objectives,
                                      @Nullable Integer clusterCountLowerBound,
                                      @Nullable Integer clusterCountUpperBound) {
        this.algorithm = algorithm;
        this.encodingType = encodingType;
        this.objectives = objectives;
        this.clusterCountLowerBound = clusterCountLowerBound;
        this.clusterCountUpperBound = clusterCountUpperBound;
    }

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

    public EvolutionaryAlgorithm getAlgorithm() {
        return algorithm;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    public Optional<Integer> getClusterCountLowerBound() {
        return Optional.ofNullable(clusterCountLowerBound);
    }

    public Optional<Integer> getClusterCountUpperBound() {
        return Optional.ofNullable(clusterCountUpperBound);
    }
}
