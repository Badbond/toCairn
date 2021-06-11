package me.soels.thesis.api.dtos;

import lombok.Getter;
import me.soels.thesis.model.EvaluationResult;

import java.util.UUID;

/**
 * The data transfer object for {@link EvaluationResult}.
 * <p>
 * This object is read-only as it is a result from an evaluation run and is therefore factual immutable information.
 */
@Getter
public class EvaluationResultDto {
    private final UUID id;

    public EvaluationResultDto(EvaluationResult result) {
        id = result.getId();
    }
}
