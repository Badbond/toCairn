package me.soels.thesis.model;

import java.util.Objects;
import java.util.UUID;

public final class DataClass extends AbstractClass {
    private final Integer size;

    /**
     * Construct an instance of a data class.
     *
     * @param identifier        the identifier for this data class (FQN)
     * @param humanReadableName the human readable name for this data class
     * @param size              the size of the data class in bytes
     * @param evaluationId      the evaluation this class is a part of
     */
    public DataClass(String identifier, String humanReadableName, Integer size, UUID evaluationId) {
        super(identifier, humanReadableName, evaluationId);
        this.size = size;
    }

    public Integer getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), size);
    }
}
