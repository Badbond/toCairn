package me.soels.thesis.tmp.daos;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Models an evaluation of a project.
 * <p>
 * The evaluation of a project contains identifiable information, the input for performing the analysis, the result
 * from analysis prior to clustering, and the results on the evaluation runs.
 * <p>
 * Note, the 'evaluation' can be seen as an analysis on the project, but to remove confusion with analyzing inputs
 * (static analysis, dynamic analysis, etc.) we have chosen 'evaluation' for its name.
 */
@Entity
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

    // TODO: Model inputs
    //  This is not project zip, git log, JFR results, etc. -- those are one-offs and should be processed immediately
    //  Instead we want to store the classes, relationships, sizes, etc.
    //  These are to be based on the configured configuration.objectives and validated as such before running

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public EvaluationConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(EvaluationConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<EvaluationResult> getResults() {
        return results;
    }

    public void setResults(List<EvaluationResult> results) {
        this.results = results;
    }
}
