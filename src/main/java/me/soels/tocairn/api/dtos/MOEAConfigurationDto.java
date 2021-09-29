package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.tocairn.model.EvolutionaryAlgorithm;
import me.soels.tocairn.model.MOEAConfiguration;
import me.soels.tocairn.solver.metric.MetricType;
import me.soels.tocairn.solver.moea.EncodingType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Optional;

@Getter
public class MOEAConfigurationDto extends SolverConfigurationDto {
    @NotNull
    private final EvolutionaryAlgorithm algorithm;

    @NotNull
    private final EncodingType encodingType;

    @NotNull
    @Size(min = 10)
    private final int maxEvaluations;

    @Size(min = 1000)
    private final Long maxTime;

    public MOEAConfigurationDto(MOEAConfiguration dao) {
        super(dao);
        this.algorithm = dao.getAlgorithm();
        this.encodingType = dao.getEncodingType();
        this.maxEvaluations = dao.getMaxEvaluations();
        this.maxTime = dao.getMaxTime().orElse(null);
    }

    @JsonCreator
    public MOEAConfigurationDto(List<MetricType> metrics, Integer minClusterAmount, Integer maxClusterAmount, EvolutionaryAlgorithm algorithm, EncodingType encodingType, int maxEvaluations, Long maxTime) {
        super(metrics, minClusterAmount, maxClusterAmount);
        this.algorithm = algorithm;
        this.encodingType = encodingType;
        this.maxEvaluations = maxEvaluations;
        this.maxTime = maxTime;
    }

    @Override
    public MOEAConfiguration toDao() {
        var dao = new MOEAConfiguration();
        dao.setAlgorithm(algorithm);
        dao.setEncodingType(encodingType);
        dao.setMaxEvaluations(maxEvaluations);
        dao.setMaxTime(maxTime);
        dao.setMetrics(getMetrics());
        dao.setMinClusterAmount(getMinClusterAmount().orElse(null));
        dao.setMaxClusterAmount(getMaxClusterAmount().orElse(null));
        return dao;
    }

    public Optional<Long> getMaxTime() {
        return Optional.ofNullable(maxTime);
    }
}
