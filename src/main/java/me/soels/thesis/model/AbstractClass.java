package me.soels.thesis.model;

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
}
