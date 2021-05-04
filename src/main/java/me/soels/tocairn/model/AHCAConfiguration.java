package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.LinkedList;
import java.util.List;

/**
 * Models the configuration of an evaluation.
 * <p>
 * The configuration contains all the information necessary to, given the input for the objectives, perform the
 * AHCA clustering. This therefore primarily configures the problem statement of the evaluation.
 * <p>
 * Note that when {@link #normalizeMetrics} is set to {@code true} that we need to know the max value of every metric
 * for every clustering in the current iteration. Thereofre, we can not perform the memory optimization of not storing
 * all clusterings. This therefore does not work on a large scale where many clusterings need to be assessed per step.
 */
@Node
@Getter
@Setter
public class AHCAConfiguration extends SolverConfiguration {
    private List<Double> weights = new LinkedList<>();
    private boolean optimizationOnSharedEdges = true;
    private boolean normalizeMetrics = true;
}

