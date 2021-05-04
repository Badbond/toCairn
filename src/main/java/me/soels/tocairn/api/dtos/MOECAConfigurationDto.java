package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import me.soels.tocairn.model.MOECAConfiguration;
import me.soels.tocairn.solver.metric.MetricType;
import me.soels.tocairn.solver.moeca.EncodingType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MOECAConfigurationDto extends SolverConfigurationDto {
    @NotNull
    private final String algorithm;

    @NotNull
    private final EncodingType encodingType;

    @NotNull
    @Size(min = 10)
    private final int maxEvaluations;

    @Size(min = 1000)
    private final Long maxTime;

    @Size(min = 1)
    private final Integer populationSize;

    private final Map<String, String> additionalProperties;

    public MOECAConfigurationDto(MOECAConfiguration dao) {
        super(dao);
        this.algorithm = dao.getAlgorithm();
        this.encodingType = dao.getEncodingType();
        this.maxEvaluations = dao.getMaxEvaluations();
        this.maxTime = dao.getMaxTime().orElse(null);
        this.populationSize = dao.getPopulationSize().orElse(null);
        this.additionalProperties = dao.getAdditionalProperties();
    }

    @JsonCreator
    public MOECAConfigurationDto(List<MetricType> metrics, Integer minClusterAmount, Integer maxClusterAmount,
                                 String algorithm, EncodingType encodingType, int maxEvaluations,
                                 Long maxTime, Integer populationSize, Map<String, String> additionalProperties) {
        super(metrics, minClusterAmount, maxClusterAmount);
        this.algorithm = algorithm;
        this.encodingType = encodingType;
        this.maxEvaluations = maxEvaluations;
        this.maxTime = maxTime;
        this.populationSize = populationSize;
        this.additionalProperties = additionalProperties == null ? new HashMap<>() : additionalProperties;
    }

    @Override
    public MOECAConfiguration toDao() {
        var dao = new MOECAConfiguration();
        dao.setAlgorithm(algorithm);
        dao.setEncodingType(encodingType);
        dao.setMaxEvaluations(maxEvaluations);
        dao.setMaxTime(maxTime);
        dao.setPopulationSize(populationSize);
        dao.setAdditionalProperties(additionalProperties);
        dao.setMetrics(getMetrics());
        dao.setMinClusterAmount(getMinClusterAmount().orElse(null));
        dao.setMaxClusterAmount(getMaxClusterAmount().orElse(null));
        return dao;
    }
}
