package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.clustering.objectives.ObjectiveType;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.*;

import java.util.*;

@Node
@Getter
@Setter
public class Solution {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @Relationship("HAS_CLUSTERS")
    private List<Cluster> clusters = new ArrayList<>();

    @CompositeProperty(prefix = "objectives")
    private Map<ObjectiveType, List<Double>> objectiveValues = new EnumMap<>(ObjectiveType.class);
}
