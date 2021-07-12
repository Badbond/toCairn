package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Indicates that there is a relationship between an {@link OtherClass} and a {@link DataClass}.
 * <p>
 * This relationships contains additional metadata to indicate which type of access is being used. This is indicated
 * by {@link #getType()}. We will set this to {@link DataRelationshipType#WRITE} when at least one call from
 * {@code caller} to {@link #getCallee()} is a modifying the data in the {@link #getCallee()}.
 */
@Getter
@Setter
public final class DataRelationship extends DependenceRelationship {
    private DataRelationshipType type;

    public DataRelationship(DataClass callee, DataRelationshipType type, int staticFrequency, Integer dynamicFrequency) {
        super(callee, staticFrequency, dynamicFrequency);
        this.type = type;
    }

    @Override
    public DataClass getCallee() {
        return (DataClass) super.getCallee();
    }
}
