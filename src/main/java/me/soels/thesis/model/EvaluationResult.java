package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Models the result of an evaluation run.
 * <p>
 * The objectives stored are for identification purposes to see which objectives were included in the run.
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

    // TODO: Model metrics (performance metrics), created date
}
