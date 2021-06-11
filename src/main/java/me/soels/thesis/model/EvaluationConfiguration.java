package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.clustering.encoding.EncodingType;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Optional;
import java.util.UUID;

/**
 * Models the configuration of an evaluation.
 * <p>
 * The configuration contains all the information necessary to, given the input for the objectives, perform the
 * multi-objective clustering. This therefore primarily configures the problem statement of the evaluation.
 */
@Node
@Getter
@Setter
public class EvaluationConfiguration {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @NotNull
    private EvolutionaryAlgorithm algorithm;

    @NotNull
    private EncodingType encodingType;

    @NotNull
    private int maxEvaluations;

    @Nullable
    private Long maxTime;

    @Nullable
    @Size(min = 1)
    private Integer clusterCountLowerBound;

    @Nullable
    @Size(min = 1)
    private Integer clusterCountUpperBound;

    // TODO: Model operators to use (enum-wise)

    public Optional<Long> getMaxTime() {
        return Optional.ofNullable(maxTime);
    }

    public Optional<Integer> getClusterCountLowerBound() {
        return Optional.ofNullable(clusterCountLowerBound);
    }

    public Optional<Integer> getClusterCountUpperBound() {
        return Optional.ofNullable(clusterCountUpperBound);
    }
}
