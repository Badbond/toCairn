package me.soels.tocairn.api.dtos;

import lombok.Getter;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.Microservice;
import me.soels.tocairn.model.Solution;
import me.soels.tocairn.solver.metric.MetricType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class SolutionDto {
    private final UUID id;
    private final Map<MetricType, double[]> objectiveValues;
    private final Map<MetricType, double[]> normalizedObjectiveValues;
    private final List<MicroserviceDto> microserviceDtos;

    public SolutionDto(Solution solution) {
        this.id = solution.getId();
        this.objectiveValues = solution.getMetricValues();
        this.normalizedObjectiveValues = solution.getNormalizedMetricValues();
        this.microserviceDtos = solution.getMicroservices().stream()
                .map(MicroserviceDto::new)
                .sorted(Comparator.comparingInt(ms -> ms.getFqns().size()))
                .collect(Collectors.toList());
    }

    @Getter
    public static class MicroserviceDto {
        private final List<String> fqns;
        private final int number;

        public MicroserviceDto(Microservice microservice) {
            this.number = microservice.getMicroserviceNumber();
            this.fqns = microservice.getClasses().stream()
                    .map(AbstractClass::getIdentifier)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
