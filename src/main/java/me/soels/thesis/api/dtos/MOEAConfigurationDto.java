package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.model.EvolutionaryAlgorithm;
import me.soels.thesis.model.MOEAConfiguration;
import me.soels.thesis.model.SolverConfiguration;
import me.soels.thesis.solver.metric.MetricType;
import me.soels.thesis.solver.moea.EncodingType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Optional;
import java.util.Set;

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

    @Size(min = 1)
    private final Integer clusterCountLowerBound;

    @Size(min = 1)
    private final Integer clusterCountUpperBound;

    public MOEAConfigurationDto(MOEAConfiguration dao) {
        super(dao.getMetrics());
        this.algorithm = dao.getAlgorithm();
        this.encodingType = dao.getEncodingType();
        this.maxEvaluations = dao.getMaxEvaluations();
        this.maxTime = dao.getMaxTime().orElse(null);
        this.clusterCountLowerBound = dao.getClusterCountLowerBound().orElse(null);
        this.clusterCountUpperBound = dao.getClusterCountUpperBound().orElse(null);
    }

    @JsonCreator
    public MOEAConfigurationDto(Set<MetricType> metrics, EvolutionaryAlgorithm algorithm, EncodingType encodingType, int maxEvaluations, Long maxTime, Integer clusterCountLowerBound, Integer clusterCountUpperBound) {
        super(metrics);
        this.algorithm = algorithm;
        this.encodingType = encodingType;
        this.maxEvaluations = maxEvaluations;
        this.maxTime = maxTime;
        this.clusterCountLowerBound = clusterCountLowerBound;
        this.clusterCountUpperBound = clusterCountUpperBound;
    }

    @Override
    public MOEAConfiguration toDao() {
        var dao = new MOEAConfiguration();
        dao.setAlgorithm(algorithm);
        dao.setEncodingType(encodingType);
        dao.setMaxEvaluations(maxEvaluations);
        dao.setMaxTime(maxTime);
        dao.getMetrics().addAll(getMetrics());
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
