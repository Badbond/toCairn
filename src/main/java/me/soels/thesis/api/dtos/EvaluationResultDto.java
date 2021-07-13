package me.soels.thesis.api.dtos;

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
@Getter
public class EvaluationResultDto {
    private final UUID id;
    private final List<UUID> solutionIds;
    private final ZonedDateTime createdDate;
    private final Double hyperVolume;
    private final Double generationalDistance;
    private final Double invertedGenerationalDistance;
    private final Double additiveEpsilonIndicator;
    private final Double maximumParetoFrontError;
    private final Double spacing;
    private final Double contribution;
    private final Double r1Indicator;
    private final Double r2Indicator;
    private final Double r3Indicator;

    public EvaluationResultDto(EvaluationResult result) {
        this.id = result.getId();
        this.solutionIds = result.getSolutions().stream().map(Solution::getId).collect(Collectors.toList());
        this.createdDate = result.getCreatedDate();
        this.hyperVolume = result.getHyperVolume();
        this.generationalDistance = result.getGenerationalDistance();
        this.invertedGenerationalDistance = result.getInvertedGenerationalDistance();
        this.additiveEpsilonIndicator = result.getAdditiveEpsilonIndicator();
        this.maximumParetoFrontError = result.getMaximumParetoFrontError();
        this.spacing = result.getSpacing();
        this.contribution = result.getContribution();
        this.r1Indicator = result.getR1Indicator();
        this.r2Indicator = result.getR2Indicator();
        this.r3Indicator = result.getR3Indicator();
    }
}
