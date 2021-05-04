package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.ahca.AHCASolver;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Models the result from a {@link AHCASolver}.
 */
@Node
@Getter
@Setter
public class AHCAEvaluationResult extends EvaluationResult {
}
