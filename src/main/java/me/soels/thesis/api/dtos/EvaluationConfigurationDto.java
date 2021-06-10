package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.clustering.encoding.EncodingType;
import me.soels.thesis.model.EvaluationConfiguration;
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

    @Size(min = 1)
    private final Integer clusterCountLowerBound;

    @Size(min = 1)
    private final Integer clusterCountUpperBound;

    public EvaluationConfigurationDto(EvaluationConfiguration dao) {
        this.algorithm = dao.getAlgorithm();
        this.encodingType = dao.getEncodingType();
        this.clusterCountLowerBound = dao.getClusterCountLowerBound().orElse(null);
        this.clusterCountUpperBound = dao.getClusterCountUpperBound().orElse(null);
    }

    public EvaluationConfiguration toDao() {
        var dao = new EvaluationConfiguration();
        dao.setAlgorithm(algorithm);
        dao.setEncodingType(encodingType);
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
