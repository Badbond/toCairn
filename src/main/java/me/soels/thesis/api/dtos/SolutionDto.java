package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.Microservice;
import me.soels.thesis.model.Solution;
import me.soels.thesis.solver.objectives.ObjectiveType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class SolutionDto {
    private final Map<ObjectiveType, double[]> objectiveValues;
    private final List<MicroserviceDto> microserviceDtos;

    public SolutionDto(Solution solution) {
        this.microserviceDtos = solution.getMicroservices().stream().map(MicroserviceDto::new).collect(Collectors.toList());
        this.objectiveValues = solution.getObjectiveValues();
    }

    @Getter
    public static class MicroserviceDto {
        private final List<String> fqns;
        private final int number;

        public MicroserviceDto(Microservice microservice) {
            this.fqns = microservice.getClasses().stream().map(AbstractClass::getIdentifier).collect(Collectors.toList());
            this.number = microservice.getMicroserviceNumber();
        }
    }
}
