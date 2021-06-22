package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Relationship between two {@link AbstractClass} that signify one depends on the other.
 * <p>
 * This relationship is used to identify non-data relationships in our graph. The {@link #getStaticFrequency()} is the number
 * of times that {@code caller} calls {@link #getCallee()} in a different location of the source code. Therefore,
 * this is constructed using static analysis. We don't use dynamic analysis as that is too costly to run for all
 * classes and methods. An exception to this is the {@link DataRelationship}.
 */
@Getter
@Setter
public class DependenceRelationship extends Relationship {
    private int staticFrequency;

    public DependenceRelationship(AbstractClass callee, int staticFrequency) {
        super(callee);
        this.staticFrequency = staticFrequency;
    }
}
