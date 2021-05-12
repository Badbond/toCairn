package me.soels.thesis;

import me.soels.thesis.encoding.EncodingType;
import me.soels.thesis.encoding.VariableDecoder;
import me.soels.thesis.encoding.VariableType;
import me.soels.thesis.model.AnalysisModel;
import me.soels.thesis.model.DependenceRelationship;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.objectives.CohesionCarvalhoObjective;
import me.soels.thesis.objectives.CouplingBetweenModuleClassesObjective;
import me.soels.thesis.objectives.Objective;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.variable.EncodingUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MOEAExperiment {
    private static final String GRAPH_NAME = "simple-graph-2";

    @Test
    public void runExperimentTest() {
        var problemConfig = new ProblemConfiguration(EncodingType.GRAPH_ADJECENCY, VariableType.FLOAT_INT, null, null);
        runExperiment(problemConfig);
    }

    private void runExperiment(ProblemConfiguration config) {
        List<Objective> objectives = List.of(new CouplingBetweenModuleClassesObjective(), new CohesionCarvalhoObjective());
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

//        Amount of non-dominated solutions: 4
//        Processing took: 0:00:11.902 (H:m:s.millis)
//        CouplingInModuleObjective  CouplingBetweenMicroservicesObjective  CouplingBetweenModulesObjective  CouplingBetweenModuleClassesObjective
//        -0.6000                    0.5500                                 1.3333                           2.6667
//        Variable values:
//        A: 3, B: 3, C: 3, D: 3, E: 0, F: 0, G: 0, H: 0, I: 0, J: 6,
//                -0.8333                    0.6667                                 2.0000                           2.0000
//        Variable values:
//        A: 3, B: 3, C: 3, D: 3, E: 0, F: 0, G: 0, H: 0, I: 6, J: 6,
//                -0.9000                    0.2000                                 1.0000                           3.0000
//        Variable values:
//        A: 3, B: 3, C: 3, D: 3, E: 3, F: 6, G: 6, H: 6, I: 6, J: 6,
//                -0.7778                    0.6111                                 2.0000                           2.6667
//        Variable values:
//        A: 6, B: 3, C: 3, D: 3, E: 0, F: 0, G: 0, H: 0, I: 6, J: 6,


//        Amount of non-dominated solutions: 5
//        Processing took: 0:00:10.841 (H:m:s.millis)
//        CouplingCarvalhoObjective  CohesionCarvalhoObjective
//        0.0000                     -1.0000
//        48.0000                     -3.0000
//        40.0000                     -2.5000
//        24.0000                     -2.0000
//        16.0000                     -1.5000


        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        System.out.println("===================================");
        System.out.println("Amount of non-dominated solutions: " + result.size());
        System.out.println("Processing took: " + duration + " (H:m:s.millis)");
        printSolutionsData(result, objectives, input, config.getEncodingType());
        System.out.println("===================================");
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

    private void printSolutionsData(NondominatedPopulation result, List<Objective> objectives, AnalysisModel input, EncodingType encodingType) {
        var objectivesNames = objectives.stream()
                .map(objective -> objective.getClass().getSimpleName())
                .collect(Collectors.toList());

        for (var solution : result) {
            System.out.println("-----------------------------------");
            System.out.format(StringUtils.join(objectivesNames, "  ") + "%n");
            for (int i = 0; i < objectivesNames.size(); i++) {
                var numberLength = Double.compare(solution.getObjective(i), 0.0) < 0 ? 7 : 6;
                var spacing = " ".repeat(objectivesNames.get(i).length() - numberLength) + "  ";
                System.out.format("%.4f" + spacing, solution.getObjective(i));
            }
            System.out.println();

            var clustering = VariableDecoder.decode(input, EncodingUtils.getInt(solution), encodingType);
            if (clustering.getByClass().size() <= 12) {
                System.out.println("Clustering:");
                clustering.getByClass().forEach((clazz, cluster) -> System.out.format("%s: %d, ", clazz.getHumanReadableName(), cluster));
                System.out.println();
            }
            System.out.println("Amount of clusters: " + clustering.getByCluster().size());
        }
    }
}
