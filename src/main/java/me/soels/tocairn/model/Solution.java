package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.metric.MetricType;
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

    @CompositeProperty(prefix = "metricValues")
    private Map<MetricType, double[]> metricValues = new EnumMap<>(MetricType.class);
}
