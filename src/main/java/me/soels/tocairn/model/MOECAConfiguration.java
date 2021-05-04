package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.moeca.EncodingType;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
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
public class MOECAConfiguration extends SolverConfiguration {
    @NotNull
    private String algorithm;

    @NotNull
    private EncodingType encodingType;

    @NotNull
    private int maxEvaluations;

    @Nullable
    private Long maxTime;

    @Nullable
    private Integer populationSize;

    @CompositeProperty(prefix = "additionalProperties")
    private Map<String, String> additionalProperties = new HashMap<>();

    public Optional<Long> getMaxTime() {
        return Optional.ofNullable(maxTime);
    }

    public Optional<Integer> getPopulationSize() {
        return Optional.ofNullable(populationSize);
    }
}
