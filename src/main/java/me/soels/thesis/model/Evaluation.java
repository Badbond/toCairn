package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * Models an evaluation of a project.
 * <p>
 * The evaluation of a project contains identifiable information, the input for performing the evaluation, the result
 * from analysis prior to clustering, and the results on multiple evaluation runs.
 * <p>
 * Note, the 'evaluation' can be seen as an analysis on the project, but to reduce confusion with analyzing inputs
 * (static analysis, dynamic analysis, etc.) we have chosen 'evaluation' for its name.
 */
@Entity
@Getter
@Setter
public class Evaluation {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @NotNull
    @OneToOne
    private EvaluationConfiguration configuration;

    @OneToMany(mappedBy = "evaluation")
    private List<EvaluationResult> results = new ArrayList<>();

    @NotNull
    @Enumerated
    @Size(min = 2)
    @Column(nullable = false)
    @ElementCollection(targetClass = Objective.class, fetch = FetchType.EAGER)
    private Set<Objective> objectives;

    // TODO: Model inputs
    //  This is not project zip, git log, JFR results, etc. -- those are one-offs and should be processed immediately
    //  Instead we want to store the classes, relationships, sizes, etc.
    //  These are to be based on the configured configuration.objectives and validated as such before running
}
