package me.soels.thesis.tmp.dtos;

import lombok.Getter;
import me.soels.thesis.tmp.daos.EvaluationResult;
import me.soels.thesis.tmp.daos.Objective;

import java.util.Set;

/**
 * The data transfer object for {@link EvaluationResult}.
 * <p>
 * This object is read-only as it is a result from an evaluation run and is therefore factual immutable information.
 */
@Getter
public class EvaluationResultDto {
    private final Set<Objective> objectives;

    public EvaluationResultDto(EvaluationResult result) {
        this.objectives = result.getObjectives();
    }
}
