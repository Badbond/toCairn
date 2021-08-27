package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.*;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * Models the result of an evaluation run.
 */
@Node
@Getter
@Setter
public class EvaluationResult {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @Relationship("HasSolution")
    private List<Solution> solutions = new ArrayList<>();

    private ZonedDateTime startDate;
    private ZonedDateTime finishDate;

    // TODO: Make hierarchy for solvers.

    @CompositeProperty(prefix = "populationMetrics")
    private Map<String, Double> populationMetrics = new HashMap<>();
}
