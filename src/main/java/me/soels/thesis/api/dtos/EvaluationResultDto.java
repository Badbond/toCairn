package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.model.Solution;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The data transfer object for {@link EvaluationResult}.
 * <p>
 * This object is read-only as it is a result from an evaluation run and is therefore factual immutable information.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MOEAEvaluationResultDto.class, name = "moea"),
        @JsonSubTypes.Type(value = HierarchicalEvaluationResultDto.class, name = "hierarchical")
})
@Getter
public class EvaluationResultDto {
    private final UUID id;
    private final List<UUID> solutionIds;
    private final ZonedDateTime startDate;
    private final ZonedDateTime finishDate;

    public EvaluationResultDto(EvaluationResult result) {
        this.id = result.getId();
        this.solutionIds = result.getSolutions().stream().map(Solution::getId).collect(Collectors.toList());
        this.startDate = result.getStartDate();
        this.finishDate = result.getFinishDate();
    }
}
