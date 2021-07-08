package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Relationship between two {@link AbstractClass} that signify one depends on the other.
 * <p>
 * This relationship is used to identify non-data relationships in our graph. The {@link #getStaticFrequency()} is the number
 * of times that {@code caller} calls {@link #getCallee()} in a different location of the source code. Therefore,
 * this is constructed using static analysis. We don't use dynamic analysis as that is too costly to run for all
 * classes and methods. An exception to this is the {@link DataRelationship}.
 * <p>
 * This relationship also holds metadata for how often {@code caller} interacts with {@link #getCallee()}.
 * We keep track of static frequency by by the amount of unique method calls from {@code caller} to
 * {@link #getCallee()}. When dynamic data is provided, this will be determined based on
 * the amount of interactions in the time frame for which dynamic analysis was performed.
 */
@Getter
@Setter
public class DependenceRelationship extends Relationship {
    private int staticFrequency;
    private Integer dynamicFrequency;

    public DependenceRelationship(AbstractClass callee, int staticFrequency, @Nullable Integer dynamicFrequency) {
        super(callee);
        this.staticFrequency = staticFrequency;
        this.dynamicFrequency = dynamicFrequency;
    }

    public Optional<Integer> getDynamicFrequency() {
        return Optional.ofNullable(dynamicFrequency);
    }
}
