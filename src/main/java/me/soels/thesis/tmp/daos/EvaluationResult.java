package me.soels.thesis.tmp.daos;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Models the result of an evaluation run.
 * <p>
 * The objectives stored are for identification purposes to see which objectives were included in the run.
 */
@Entity
public class EvaluationResult {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @NotNull
    @ManyToOne(optional = false)
    private Evaluation evaluation;

    @NotNull
    @Enumerated
    @Size(min = 2)
    @Column(nullable = false)
    @ElementCollection(targetClass = Objective.class, fetch = FetchType.EAGER)
    private List<Objective> objectives;

    // TODO: Model metrics (performance metrics)
    // TODO: Model (linkage to) clustering solutions

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<Objective> objectives) {
        this.objectives = objectives;
    }
}
