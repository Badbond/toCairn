package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.moea.MOEASolver;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Models the result from a {@link MOEASolver}.
 */
@Node
@Getter
@Setter
public class MOEAEvaluationResult extends EvaluationResult {
    @CompositeProperty(prefix = "populationMetrics")
    private Map<String, Double> populationMetrics = new HashMap<>();
}
