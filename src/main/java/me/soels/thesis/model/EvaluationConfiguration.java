package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.clustering.encoding.EncodingType;
import org.hibernate.annotations.GenericGenerator;

import javax.annotation.Nullable;
import javax.persistence.*;
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
@Entity
@Getter
@Setter
public class EvaluationConfiguration {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @OneToOne(mappedBy = "configuration")
    private Evaluation evaluation;

    @NotNull
    @Enumerated
    @Column(nullable = false)
    private EvolutionaryAlgorithm algorithm;

    @NotNull
    @Column(nullable = false)
    private EncodingType encodingType;

    @NotNull
    @Column(nullable = false)
    private int maxEvaluations;

    @Column
    @Nullable
    private Long maxTime;

    @Column
    @Nullable
    @Size(min = 1)
    private Integer clusterCountLowerBound;

    @Column
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
