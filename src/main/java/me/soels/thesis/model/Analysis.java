package me.soels.thesis.model;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.UUID;


/**
 * Models analysis of a project.
 * <p>
 * The analysis of a project contains identifiable information, the input for performing the analysis,
 * metrics on the analysis and the output of the analysis.
 */
@Entity
@Validated
public class Analysis {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Nonnull
    @Column(nullable = false)
    private String name;

    @Nonnull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Nonnull
    public UUID getId() {
        return id;
    }

    public void setId(@Nonnull UUID id) {
        this.id = id;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(@Nonnull AnalysisStatus status) {
        this.status = status;
    }
}
