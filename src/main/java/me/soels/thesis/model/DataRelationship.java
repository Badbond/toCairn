package me.soels.thesis.model;

/**
 * Indicates that there is a relationship between an {@link OtherClass} and a {@link DataClass}.
 * <p>
 * This relationships contains additional metadata to indicate which type of access is being used. This is indicated
 * by {@link #getType()}. We will set this to {@link DataRelationshipType#WRITE} when at least one call from
 * {@link #getCaller()} to {@link #getCallee()} is a modifying the data in the {@link #getCallee()}.
 * <p>
 * This relationship also holds additional metadata for how often {@link #getCaller()} interacts with
 * {@link #getCallee()}. If only static analysis is used, this will be determined by the amount of unique method calls
 * from {@link #getCaller()} to {@link #getCallee()}. When dynamic analysis is used, this will be determined based on
 * the amount of interactions in the time frame for which dynamic analysis was performed.
 */
public final class DataRelationship extends Relationship {
    private final DataRelationshipType type;
    private final int frequency;

    public DataRelationship(OtherClass otherClass, DataClass dataClass, DataRelationshipType type, int frequency) {
        super(otherClass, dataClass);
        this.type = type;
        this.frequency = frequency;
    }

    public DataRelationshipType getType() {
        return type;
    }

    public int getFrequency() {
        return frequency;
    }

    @Override
    public OtherClass getCaller() {
        return (OtherClass) super.getCaller();
    }

    @Override
    public DataClass getCallee() {
        return (DataClass) super.getCallee();
    }
}
