package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.moea.EncodingType;
import org.springframework.data.neo4j.core.schema.Node;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
public class MOEAConfiguration extends SolverConfiguration {
    @NotNull
    private EvolutionaryAlgorithm algorithm;

    @NotNull
    private EncodingType encodingType;

    @NotNull
    private int maxEvaluations;

    @Nullable
    private Long maxTime;

    public Optional<Long> getMaxTime() {
        return Optional.ofNullable(maxTime);
    }
}
