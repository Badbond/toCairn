package me.soels.tocairn.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
public class CustomSolutionDto {
    private final List<CustomMicroserviceDto> microservices;

    @JsonCreator
    public CustomSolutionDto(List<CustomMicroserviceDto> microservices) {
        this.microservices = microservices;
    }

    @Getter
    public static class CustomMicroserviceDto {
        private final Set<String> fqns;

        @JsonCreator
        public CustomMicroserviceDto(Set<String> fqns) {
            this.fqns = fqns;
        }
    }
}
