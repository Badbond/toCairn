package me.soels.thesis;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MOEAExperiment {
    @Test
    public void runExperiment() {
        var objectives = List.of(new CohesionObjective(), new CouplingObjective());
        var input = prepareInput();
        var start = System.currentTimeMillis();

        // TODO:
        //  Instead of random, we want to make smarter initialization by doing the following:
        //      - Select n random classes and start traversing every class not already visited
        //      - Unvisited classes get assigned a random cluster.
        //  This is hardcoded in the AlgorithmProviders. Therefore, we need to think of some injection (e.g. aspects /
        //  custom providers). See https://github.com/MOEAFramework/MOEAFramework/issues/51#issuecomment-223448440

        // TODO:
        //  Further investigate control over duplicates. Desirable:
        //      - Being able to normalize the solution
        //      - Have non-duplicated solutions (results in same clustering)
        //      - Allow duplicated objectives (same result, different clustering, is still interesting)
        NondominatedPopulation result = new Executor()
                // Quick experimenting shows that as of 2021-05-06 cluster label was 14K nano sec per eval avg and
                // graph adjacency was 27K. However, graph adjacency has tighter clustering in initial population.
                .withProblem(new ClusteringProblem(objectives, input, EncodingType.CLUSTER_LABEL))
                .withAlgorithm("NSGAII")
                // TODO: Operators; crossover & mutation. By default for NSGA-II it is Simulated Binary Crossover (SBX) and Polynomial Mutation (PM).
                .distributeOnAllCores()
                .withMaxEvaluations(10000)
                .run();

        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        System.out.println("Processing took: " + duration + " (H:m:s.millis)");
        printResults(result, objectives);
    }

    private ApplicationInput prepareInput() {
        var graph = getGraph();
        return new ApplicationInput(graph.getKey(), graph.getValue());
    }

    private Pair<List<String>, List<Pair<String, String>>> getGraph() {
        var graphLines = getGraphString().split("\n");
        var classMapping = new LinkedHashMap<String, String>();
        var edges = new ArrayList<Pair<String, String>>();
        for (var line : graphLines) {
            var split = Arrays.stream(line.split(","))
                    .map(StringUtils::trim)
                    .collect(Collectors.toList());
            classMapping.putIfAbsent(split.get(0), "Class" + classMapping.size());
            classMapping.putIfAbsent(split.get(1), "Class" + classMapping.size());
            edges.add(new ImmutablePair<>(classMapping.get(split.get(0)), classMapping.get(split.get(1))));
        }
        return new ImmutablePair<>(new ArrayList<>(classMapping.values()), edges);
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

    private void printResults(NondominatedPopulation result, List<Objective> objectives) {
        var objectivesNames = objectives.stream()
                .map(objective -> objective.getClass().getSimpleName())
                .collect(Collectors.toList());
        System.out.format(StringUtils.join(objectivesNames, "  ") + "%n");

        for (var solution : result) {
            for (int i = 0; i < objectivesNames.size(); i++) {
                var spacing = " ".repeat(objectivesNames.get(i).length() - 6) + "  ";
                System.out.format("%.4f" + spacing, solution.getObjective(i));
            }
            System.out.println();
        }
    }
}
