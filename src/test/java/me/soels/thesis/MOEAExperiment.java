package me.soels.thesis;

import me.soels.thesis.encoding.EncodingType;
import me.soels.thesis.encoding.VariableType;
import me.soels.thesis.model.AnalysisModel;
import me.soels.thesis.model.DependenceRelationship;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.objectives.CohesionObjective;
import me.soels.thesis.objectives.CouplingBetweenMicroservicesObjective;
import me.soels.thesis.objectives.Objective;
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
    private static final String GRAPH_NAME = "disease-graph";

    @Test
    public void runExperimentTest() {
//        System.out.println("Graph Adjacency binary int");
//        var problemConfig = new ProblemConfiguration(EncodingType.GRAPH_ADJECENCY, VariableType.BINARY_INT, null, null);
//        runExperiment(problemConfig);
//        System.out.println("Graph Adjacency float int");
//        problemConfig = new ProblemConfiguration(EncodingType.GRAPH_ADJECENCY, VariableType.FLOAT_INT, null, null);
//        runExperiment(problemConfig);
        System.out.println("Cluster label binary int");
        var problemConfig = new ProblemConfiguration(EncodingType.CLUSTER_LABEL, VariableType.BINARY_INT, null, null);
        runExperiment(problemConfig);
        System.out.println("Cluster label float int");
        problemConfig = new ProblemConfiguration(EncodingType.CLUSTER_LABEL, VariableType.FLOAT_INT, null, null);
        runExperiment(problemConfig);
    }

    private void runExperiment(ProblemConfiguration config) {
        var objectives = List.of(new CohesionObjective(), new CouplingBetweenMicroservicesObjective());
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
                .withProblem(new ClusteringProblem(objectives, input, config))
                .withAlgorithm("NSGAII")
                .distributeOnAllCores()
                .withMaxEvaluations(1000000)
                .run();
        // Adjacency binary int:        02:02:280   01:55:095   01:53:054   02:03:669
        // Adjacency float int:         00:24:858   00:21:758   00:24:349   00:23:250
        // Cluster label binary int:    02:19:527   02:15:122   02.16.068   02:17:736   02:17:129   02:10:882   02:13:974
        // Cluster label float int:     00:26:033   00:26:522   00.26.329   00:27:119   00:25:476   00:25:729   00:26.789

        // Adjacency binary int:        0.0
        // Adjacency float int:         0.0
        // Cluster label binary int:    3.9234  3.7054  3.7052  3.7097  3.8512
        // Cluster label float int:     2.2551  2.4214  2.3813  2.7004  2.3505

        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        System.out.println("Processing took: " + duration + " (H:m:s.millis)");
        printResults(result, objectives);
    }

    private AnalysisModel prepareInput() {
        var graph = getGraph();
        return new AnalysisModel(graph.getKey(), graph.getValue());
    }

    private Pair<List<OtherClass>, List<DependenceRelationship>> getGraph() {
        var graphLines = Arrays.stream(getGraphString().split("\n"))
                .skip(2)
                .collect(Collectors.toList());
        var classMapping = new LinkedHashMap<String, OtherClass>();
        var edges = new ArrayList<DependenceRelationship>();
        for (var line : graphLines) {
            var split = Arrays.stream(line.split(","))
                    .map(StringUtils::trim)
                    .collect(Collectors.toList());
            var classA = classMapping.computeIfAbsent(split.get(0), key -> new OtherClass("Class" + classMapping.size(), split.get(0)));
            var classB = classMapping.computeIfAbsent(split.get(1), key -> new OtherClass("Class" + classMapping.size(), split.get(1)));
            edges.add(new DependenceRelationship(classA, classB));
        }
        return new ImmutablePair<>(new ArrayList<>(classMapping.values()), edges);
    }

    private String getGraphString() {
        try {
            var stream = this.getClass().getClassLoader().getResourceAsStream(GRAPH_NAME + ".csv");
            if (stream == null) {
                throw new IllegalStateException("Could not find " + GRAPH_NAME + ".csv file");
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
                var numberLength = Double.compare(solution.getObjective(i), 0.0) < 0 ? 7 : 6;
                var spacing = " ".repeat(objectivesNames.get(i).length() - numberLength) + "  ";
                System.out.format("%.4f" + spacing, solution.getObjective(i));
            }
            System.out.println();
        }
    }
}
