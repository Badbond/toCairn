package me.soels.thesis.controllers.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import me.soels.thesis.model.Analysis;
import me.soels.thesis.model.AnalysisStatus;

import java.util.UUID;

public class AnalysisDto {
    private final UUID id;
    private final String name;
    private final AnalysisStatus status;

    @JsonCreator
    public AnalysisDto(String name) {
        this.name = name;

        // Non-settable properties by user
        this.id = null;
        this.status = null;
    }

    public AnalysisDto(Analysis dao) {
        this.id = dao.getId();
        this.name = dao.getName();
        this.status = dao.getStatus();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public Analysis toDao() {
        var dao = new Analysis();
        dao.setName(name);
        return dao;
    }
}
