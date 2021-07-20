package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
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
    // TODO: Check whether all information is present to execute metrics proposed by Selmadji as they are based on
    //  the interaction between methods of a pair of classes.
    @NotNull
    private int staticFrequency;
    private Integer dynamicFrequency;

    public DependenceRelationship(AbstractClass callee, int staticFrequency, Integer dynamicFrequency) {
        super(callee);
        this.staticFrequency = staticFrequency;
        this.dynamicFrequency = dynamicFrequency;
    }

    public Optional<Integer> getDynamicFrequency() {
        // We could not store the relationship with optional properties and thus set it to -1 instead for not present
        return Optional.ofNullable(dynamicFrequency);
    }
}
