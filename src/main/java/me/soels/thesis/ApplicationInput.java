package me.soels.thesis;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;

/**
 * The input for the multi-objective evolutionary algorithm.
 * <p>
 * This input is to be constructed from initial analysis of the application that we wish to identify microservice
 * boundaries for. It has to contain all the necessary information to calculate the fitness of the clustering generated
 * by the algorithm for the given objective function.
 * <p>
 * The data in this class is to be immutable so that it can be shared among solution evaluations.
 *
 * @see Objective
 */
// TODO:
//  Questions left open: How do we model data objects? How do we incorporate additional information such as data size
//                       and dependency frequency? Should we model without Strings?
public final class ApplicationInput {
    private final List<String> classes;
    private final List<Pair<String, String>> edges;

    public ApplicationInput(List<String> classes, List<Pair<String, String>> edges) {
        this.classes = Collections.unmodifiableList(classes);
        this.edges = Collections.unmodifiableList(edges);
    }

    public List<String> getClasses() {
        return classes;
    }

    public List<Pair<String, String>> getEdges() {
        return edges;
    }
}
