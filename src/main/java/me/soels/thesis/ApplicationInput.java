package me.soels.thesis;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The input for the multi-objective evolutionary algorithm.
 * <p>
 * This input is to be constructed from initial analysis of the application that we wish to identify microservice
 * boundaries for.
 */
// TODO:
//  This class was created as mock input data. We should determine whether the current structure is acceptable.
//  Questions left open: How do we model data objects? How do we incorporate additional information such as data size
//                       and dependency frequency? Should we model without Strings?
public class ApplicationInput {
    private final List<String> classes = new ArrayList<>();
    private final List<Pair<String, String>> edges = new ArrayList<>();

    public ApplicationInput() {
        // Temporary hard-coded graph from graph.csv
        constructGraph();
    }

    private void constructGraph() {
        var graphLines = getGraphString().split("\n");
        var classMapping = new LinkedHashMap<String, String>();
        for (var line : graphLines) {
            var split = Arrays.stream(line.split(","))
                    .map(StringUtils::trim)
                    .collect(Collectors.toList());
            classMapping.putIfAbsent(split.get(0), "Class" + classMapping.size());
            classMapping.putIfAbsent(split.get(1), "Class" + classMapping.size());
            edges.add(new ImmutablePair<>(classMapping.get(split.get(0)), classMapping.get(split.get(1))));
        }
        classes.addAll(classMapping.values());
    }

    private String getGraphString() {
        try {
            // Based off of https://infranodus.com/diseases/diseases
            var stream = this.getClass().getClassLoader().getResourceAsStream("graph.csv");
            if (stream == null) {
                throw new IllegalStateException("Could not find graph.csv file");
            }
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not convert InputStream to String", e);
        }
    }

    public List<String> getClasses() {
        return classes;
    }

    public List<Pair<String, String>> getEdges() {
        return edges;
    }
}
