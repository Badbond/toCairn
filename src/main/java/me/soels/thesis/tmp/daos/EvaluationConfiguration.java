package me.soels.thesis.tmp.daos;

import me.soels.thesis.encoding.EncodingType;
import org.hibernate.annotations.GenericGenerator;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Column
    @Nullable
    @Size(min = 1)
    private Integer clusterCountLowerBound;

    @Column
    @Nullable
    @Size(min = 1)
    private Integer clusterCountUpperBound;

    @NotNull
    @Enumerated
    @Size(min = 2)
    @Column(nullable = false)
    @ElementCollection(targetClass = Objective.class, fetch = FetchType.EAGER)
    private List<Objective> objectives;

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

    public List<Objective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<Objective> objectives) {
        this.objectives = objectives;
    }
}
