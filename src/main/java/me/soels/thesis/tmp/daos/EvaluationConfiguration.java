package me.soels.thesis.tmp.daos;

import me.soels.thesis.encoding.EncodingType;
import me.soels.thesis.tmp.dtos.EvaluationConfigurationDto;
import org.hibernate.annotations.GenericGenerator;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Models the configuration of an evaluation.
 * <p>
 * The configuration contains all the information necessary to, given the input for the objectives, perform the
 * multi-objective clustering. This therefore primarily configures the problem statement of the evaluation.
 */
@Entity
public class EvaluationConfiguration {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @NotNull
    @Enumerated
    @Column(nullable = false)
    private EvolutionaryAlgorithm algorithm;

    @NotNull
    @Column(nullable = false)
    private EncodingType encodingType;

    @NotNull
    @Enumerated
    @Size(min = 2)
    @Column(nullable = false)
    @ElementCollection(targetClass = Objective.class, fetch = FetchType.EAGER)
    private List<Objective> objectives;

    @NotNull
    @Column(nullable = false)
    private int maxEvaluations;

    @Column
    @Nullable
    @Size(min = 1)
    private Integer clusterCountLowerBound;

    @Column
    @Nullable
    @Size(min = 1)
    private Integer clusterCountUpperBound;

    // TODO: Model max evaluations to accept, max time, operators to use (enum-wise)

    public UUID getId() {
        return id;
    }

    public void setId(UUID uuid) {
        this.id = uuid;
    }

    public EvolutionaryAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(EvolutionaryAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<Objective> objectives) {
        this.objectives = objectives;
    }

    public int getMaxEvaluations() {
        return maxEvaluations;
    }

    public void setMaxEvaluations(int maxEvaluations) {
        this.maxEvaluations = maxEvaluations;
    }

    public Optional<Integer> getClusterCountLowerBound() {
        return Optional.ofNullable(clusterCountLowerBound);
    }

    public void setClusterCountLowerBound(@Nullable Integer clusterCountLowerBound) {
        this.clusterCountLowerBound = clusterCountLowerBound;
    }

    public Optional<Integer> getClusterCountUpperBound() {
        return Optional.ofNullable(clusterCountUpperBound);
    }

    public void setClusterCountUpperBound(@Nullable Integer clusterCountUpperBound) {
        this.clusterCountUpperBound = clusterCountUpperBound;
    }
}
