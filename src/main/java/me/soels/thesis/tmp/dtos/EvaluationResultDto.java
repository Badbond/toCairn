package me.soels.thesis.tmp.dtos;

import lombok.Getter;
import me.soels.thesis.tmp.daos.EvaluationResult;

/**
 * The data transfer object for {@link EvaluationResult}.
 * <p>
 * This object is read-only as it is a result from an evaluation run and is therefore factual immutable information.
 */
@Getter
public class EvaluationResultDto {
    public EvaluationResultDto(EvaluationResult result) {
    }
}
