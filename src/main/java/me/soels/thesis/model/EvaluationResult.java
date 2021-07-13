package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Relationship("HAS_SOLUTIONS")
    private List<Solution> solutions = new ArrayList<>();

    private ZonedDateTime finishDate;
    private Double hyperVolume;
    //    TODO: Or use @CompositeProperty(prefix = "metrics")
    private ZonedDateTime startDate;
    private Double generationalDistance;
    private Double invertedGenerationalDistance;
    private Double additiveEpsilonIndicator;
    private Double maximumParetoFrontError;
    private Double spacing;
    private Double contribution;
    private Double r1Indicator;
    private Double r2Indicator;
    private Double r3Indicator;
}
