package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
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

    // TODO: Model metrics (performance metrics)
    // TODO: Model (linkage to) clustering solutions -- This can be difficult as it is linked to our input model.
    //  What happens if the evaluation changes? Clear the results?
}
