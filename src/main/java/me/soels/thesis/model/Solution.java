package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;
import java.util.UUID;


/**
 * Models an evaluation of a project.
 * <p>
 * The evaluation of a project contains identifiable information, the input for performing the evaluation retrieved
 * from analysis and the results on multiple evaluation runs.
 * <p>
 * Note, the 'evaluation' can be seen as an analysis on the project, but to reduce confusion with analyzing inputs
 * (static analysis, dynamic analysis, etc.) we have chosen 'evaluation' for its name.
 */
@Node
@Getter
@Setter
public class Solution {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @Relationship("HAS_CLUSTERS")
    private List<Cluster> clusters;

    // TODO: Objective values
}
