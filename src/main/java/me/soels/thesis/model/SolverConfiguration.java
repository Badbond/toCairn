package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.solver.Solver;
import me.soels.thesis.solver.metric.MetricType;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Models the configuration of a {@link Solver}.
 */
@Node
@Getter
@Setter
public abstract class SolverConfiguration {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    private List<MetricType> metrics = new ArrayList<>();
}
