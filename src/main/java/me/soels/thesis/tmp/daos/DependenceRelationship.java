package me.soels.thesis.tmp.daos;

import lombok.Getter;
import lombok.Setter;

/**
 * Relationship between two {@link AbstractClass} that signify one depends on the other.
 * <p>
 * This relationship is used to identify non-data relationships in our graph. The {@link #getFrequency()} is the number
 * of times that {@code caller} calls {@link #getCallee()} in a different location of the source code. Therefore,
 * this is constructed using static analysis. We don't use dynamic analysis as that is too costly to run for all
 * classes and methods. An exception to this is the {@link DataRelationship}.
 */
@Getter
@Setter
public class DependenceRelationship extends Relationship {
    private int frequency;

    public DependenceRelationship(AbstractClass callee, int frequency) {
        super(callee);
        this.frequency = frequency;
    }
}
