package me.soels.thesis;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * The input for the multi-objective evolutionary algorithm.
 * <p>
 * This input is to be constructed from initial analysis of the application that we wish to identify microservice
 * boundaries for. It has to contain all the necessary information to calculate the fitness of the clustering generated
 * by the algorithm for the given objective function.
 *
 * @see Objective
 */
// TODO:
//  Questions left open: How do we model data objects? How do we incorporate additional information such as data size
//                       and dependency frequency? Should we model without Strings?
public final class ApplicationInput {
    private final List<String> classes = new ArrayList<>();
    private final List<Pair<String, String>> edges = new ArrayList<>();

    public List<String> getClasses() {
        return classes;
    }

    public List<Pair<String, String>> getEdges() {
        return edges;
    }
}
