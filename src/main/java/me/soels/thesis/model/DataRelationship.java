package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

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

    public DataRelationship(DataClass callee, DataRelationshipType type, int staticFrequency, @Nullable Integer dynamicFreq) {
        super(callee, staticFrequency, dynamicFreq);
        this.type = type;
    }

    @Override
    public DataClass getCallee() {
        return (DataClass) super.getCallee();
    }
}
