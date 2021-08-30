package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Models the configuration of an evaluation.
 * <p>
 * The configuration contains all the information necessary to, given the input for the objectives, perform the
 * multi-objective clustering. This therefore primarily configures the problem statement of the evaluation.
 */
@Node
@Getter
@Setter
public class HierarchicalConfiguration extends SolverConfiguration {
    private List<Double> weights = new LinkedList<>();
    private Integer nrClusters;

    public Optional<Integer> getNrClusters() {
        return Optional.ofNullable(nrClusters);
    }
}

