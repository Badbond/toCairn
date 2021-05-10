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
