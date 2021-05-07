package me.soels.thesis.model;

/**
 * Indicates that there is a relationship between an {@link OtherClass} and a {@link DataClass}.
 */
public final class DataRelationship {
    private final DataClass dataClass;
    private final OtherClass otherClass;
    private final DataRelationshipType type;
    private final int frequency;

    /**
     * Create a relationship between a data class and another (non-data) class.
     *
     * @param dataClass  the data class depending upon
     * @param otherClass the functional class depending on the data class
     * @param type       the type of relationship
     * @param frequency  how often this relationship has been used
     */
    public DataRelationship(DataClass dataClass, OtherClass otherClass, DataRelationshipType type, int frequency) {
        this.dataClass = dataClass;
        this.otherClass = otherClass;
        this.type = type;
        this.frequency = frequency;
    }

    public DataClass getDataClass() {
        return dataClass;
    }

    public OtherClass getOtherClass() {
        return otherClass;
    }

    public DataRelationshipType getType() {
        return type;
    }

    public int getFrequency() {
        return frequency;
    }
}
