package me.soels.thesis.model;

public final class DataClass extends AbstractClass {
    private final int size;

    /**
     * Construct an instance of a data class.
     *
     * @param identifier        the identifier for this data class (FQN)
     * @param humanReadableName the human readable name for this data class
     * @param size              the size of the data class in bytes
     */
    public DataClass(String identifier, String humanReadableName, int size) {
        super(identifier, humanReadableName);
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
