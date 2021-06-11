package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * Models an evaluation of a project.
 * <p>
 * The evaluation of a project contains identifiable information, the input for performing the evaluation, the result
 * from analysis prior to clustering, and the results on multiple evaluation runs.
 * <p>
 * Note, the 'evaluation' can be seen as an analysis on the project, but to reduce confusion with analyzing inputs
 * (static analysis, dynamic analysis, etc.) we have chosen 'evaluation' for its name.
 */
@Node
@Getter
@Setter
public class Evaluation {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    private EvaluationStatus status;

    @NotNull
    @Relationship(value = "CONFIGURED_WITH")
    private EvaluationConfiguration configuration;

    @Relationship(value = "HAS_RESULTS")
    private List<EvaluationResult> results = new ArrayList<>();

    @NotNull
    @Relationship(value = "HAS_INPUTS")
    private List<? extends AbstractClass> inputs = new ArrayList<>();

    @NotNull
    @Size(min = 2)
    private Set<Objective> objectives;
}
