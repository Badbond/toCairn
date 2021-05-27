package me.soels.thesis.model;

import java.util.Objects;

public abstract class AbstractClass {
    private final String identifier;
    private final String humanReadableName;

    protected AbstractClass(String identifier, String humanReadableName) {
        this.identifier = identifier;
        this.humanReadableName = humanReadableName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    /**
     * Returns whether the provided class is equal to this class.
     * <p>
     * Note that we only use our {@code identifier} for equality checks.
     *
     * @param o the class to check equality with
     * @return whether the given class is equal to this class
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractClass that = (AbstractClass) o;
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, humanReadableName);
    }
}
