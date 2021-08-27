package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.solver.metric.MetricType;
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

    @Relationship("HasMicroservice")
    private List<Microservice> microservices = new ArrayList<>();
    // TODO: Rename relationships based on edges in thesis.
    @CompositeProperty(prefix = "objectives")
    private Map<MetricType, double[]> objectiveValues = new EnumMap<>(MetricType.class);
}
