package me.soels.thesis.tmp.dtos;

import me.soels.thesis.tmp.daos.EvaluationResult;
import me.soels.thesis.tmp.daos.Objective;

import java.util.List;

/**
 * The data transfer object for {@link EvaluationResult}.
 * <p>
 * This object is read-only as it is a result from an evaluation run and is therefore factual immutable information.
 */
public class EvaluationResultDto {
    private final List<Objective> objectives;

    public EvaluationResultDto(EvaluationResult result) {
        this.objectives = result.getObjectives();
    }

    public List<Objective> getObjectives() {
        return objectives;
    }
}
