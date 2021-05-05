package me.soels.thesis;

import java.util.ArrayList;
import java.util.List;

/**
 * The input for the multi-objective evolutionary algorithm.
 * <p>
 * This input is to be constructed from initial analysis of the application that we wish to identify microservice
 * boundaries for.
 */
public class ApplicationInput {
    private final List<String> classes;

    public ApplicationInput() {
        this.classes = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            classes.add("Class" + i);
        }
    }
    // TODO:
    //  Implement the following in mock for now:
    //      - Implement graph structure, is in-memory fine?
    //      - Classes as nodes, dependencies as edges
    //      - What about data objects?
    //      - How do we incorporate additional information such as data size and dependency frequency?

    public List<String> getClasses() {
        return classes;
    }
}
