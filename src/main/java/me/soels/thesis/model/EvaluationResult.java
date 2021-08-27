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
public abstract class EvaluationResult {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @Relationship("HasSolution")
    private List<Solution> solutions = new ArrayList<>();

    private ZonedDateTime startDate;
    private ZonedDateTime finishDate;
}
