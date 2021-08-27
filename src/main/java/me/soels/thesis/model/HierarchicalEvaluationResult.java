package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.solver.hierarchical.HierarchicalSolver;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Models the result from a {@link HierarchicalSolver}.
 */
@Node
@Getter
@Setter
public class HierarchicalEvaluationResult extends EvaluationResult {
}
