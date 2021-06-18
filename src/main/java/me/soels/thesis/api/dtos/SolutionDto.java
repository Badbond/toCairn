package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.clustering.objectives.ObjectiveType;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.Cluster;
import me.soels.thesis.model.Solution;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class SolutionDto {
    private final Map<ObjectiveType, double[]> objectiveValues;
    private final List<ClusterDto> clusterDtos;

    public SolutionDto(Solution solution) {
        this.clusterDtos = solution.getClusters().stream().map(ClusterDto::new).collect(Collectors.toList());
        this.objectiveValues = solution.getObjectiveValues();
    }

    @Getter
    public static class ClusterDto {
        private final List<String> fqns;
        private final int number;

        public ClusterDto(Cluster cluster) {
            this.fqns = cluster.getNodes().stream().map(AbstractClass::getIdentifier).collect(Collectors.toList());
            this.number = cluster.getClusterNumber();
        }
    }
}
