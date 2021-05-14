package me.soels.thesis;

import me.soels.thesis.encoding.EncodingType;
import me.soels.thesis.encoding.VariableDecoder;
import me.soels.thesis.encoding.VariableType;
import me.soels.thesis.model.AnalysisModel;
import me.soels.thesis.model.AnalysisModelBuilder;
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
    private static final String GRAPH_NAME = "disease-graph";

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

        // TODO for Friday: Try to disable crossover operators but only allow for mutation operators. Currently SBX and PM is enabled.
        NondominatedPopulation result = new Executor()
                .withProblem(new ClusteringProblem(objectives, input, config))
                .withAlgorithm("NSGAII")
                .distributeOnAllCores()
                .withMaxEvaluations(1000000)
                .run();

        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        System.out.println("===================================");
        System.out.println("Amount of non-dominated solutions: " + result.size());
        System.out.println("Processing took: " + duration + " (H:m:s.millis)");
        printSolutionsData(result, objectives, input, config.getEncodingType());
        System.out.println("===================================");
    }

    private AnalysisModel prepareInput() {
        var builder = new AnalysisModelBuilder();
        var graph = getGraph();
        builder.withOtherClasses(graph.getKey());
        builder.withDependencies(graph.getValue());
        return builder.build();
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
