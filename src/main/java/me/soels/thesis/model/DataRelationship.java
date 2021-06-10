package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Indicates that there is a relationship between an {@link OtherClass} and a {@link DataClass}.
 * <p>
 * This relationships contains additional metadata to indicate which type of access is being used. This is indicated
 * by {@link #getType()}. We will set this to {@link DataRelationshipType#WRITE} when at least one call from
 * {@code caller} to {@link #getCallee()} is a modifying the data in the {@link #getCallee()}.
 * <p>
 * This relationship also holds additional metadata for how often {@code caller} interacts with
 * {@link #getCallee()}. If only static analysis is used, this will be determined by the amount of unique method calls
 * from {@code caller} to {@link #getCallee()}. When dynamic analysis is used, this will be determined based on
 * the amount of interactions in the time frame for which dynamic analysis was performed.
 */
@Getter
@Setter
public final class DataRelationship extends DependenceRelationship {
    private DataRelationshipType type;

    public DataRelationship(DataClass dataClass, DataRelationshipType type, int frequency) {
        super(dataClass, frequency);
        this.type = type;
    }

    @Override
    public DataClass getCallee() {
        return (DataClass) super.getCallee();
    }
}
