package me.soels.thesis.tmp.daos;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/**
 * Models the result of an evaluation run.
 * <p>
 * The objectives stored are for identification purposes to see which objectives were included in the run.
 */
@Entity
@Getter
@Setter
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
    private Set<Objective> objectives;

    // TODO: Model metrics (performance metrics)
    // TODO: Model (linkage to) clustering solutions -- This can be difficult as it is linked to our input model.
    //  What happens if the evaluation changes? Clear the results?
}
