package me.soels.thesis.model;

public final class DependenceRelationship {
    private final OtherClass first;
    private final OtherClass second;

    public DependenceRelationship(OtherClass first, OtherClass second) {
        this.first = first;
        this.second = second;
    }

    public OtherClass getFirst() {
        return first;
    }

    public OtherClass getSecond() {
        return second;
    }
}
