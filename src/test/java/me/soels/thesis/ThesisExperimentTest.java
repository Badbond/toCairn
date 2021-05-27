package me.soels.thesis;

import me.soels.thesis.analysis.StaticAnalysis;
import me.soels.thesis.analysis.StaticAnalysisInput;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;

@SpringBootTest
public class ThesisExperimentTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThesisExperimentTest.class);
    private static final String MOCK_GRAPH_NAME = "simple-graph-2";
    private static final String ZIP_FILE = "big-project-cleaned.zip";

    @Autowired
    private VariableDecoder variableDecoder;
    @Autowired
    private StaticAnalysis staticAnalysis;

    @Test
    public void runExperimentTestWithMockData() {
        var problemConfig = new ProblemConfiguration(EncodingType.CLUSTER_LABEL, VariableType.FLOAT_INT, null, null);
        var input = prepareMockInput();
        runExperiment(problemConfig, input);
    }

    @Test
    public void runExperimentWithSourceCode() throws URISyntaxException {
        var problemConfig = new ProblemConfiguration(EncodingType.CLUSTER_LABEL, VariableType.FLOAT_INT, null, null);
        var input = getZipInput();
        runExperiment(problemConfig, input);
    }

    private AnalysisModel getZipInput() throws URISyntaxException {
        var project = Path.of(this.getClass().getClassLoader().getResource(ZIP_FILE).toURI());
        var analysisInput = new StaticAnalysisInput(project, JAVA_11, null);
        var modelBuilder = new AnalysisModelBuilder();
        staticAnalysis.analyze(modelBuilder, analysisInput);
        return modelBuilder.build();
    }

    private void runExperiment(ProblemConfiguration config, AnalysisModel input) {
        List<Objective> objectives = List.of(new CouplingBetweenModuleClassesObjective(), new CohesionCarvalhoObjective());
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

        // TODO: As suggested by Carvalho et al., try to disable crossover operators but only allow for mutation
        //  operators. Currently SBX and PM is enabled.
        NondominatedPopulation result = new Executor()
                .withProblem(new ClusteringProblem(objectives, input, config))
                .withAlgorithm("NSGAII")
                .distributeOnAllCores()
                .withMaxEvaluations(1000000)
                .run();

        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("===================================");
        LOGGER.info("Amount of non-dominated solutions: " + result.size());
        LOGGER.info("Processing took: " + duration + " (H:m:s.millis)");
        printSolutionsData(result, objectives, input, config.getEncodingType());
        LOGGER.info("===================================");
    }

    private AnalysisModel prepareMockInput() {
        var builder = new AnalysisModelBuilder();
        var graph = getMockGraph();
        builder.withOtherClasses(graph.getKey());
        builder.withDependencies(graph.getValue());
        return builder.build();
    }

    private Pair<List<OtherClass>, List<DependenceRelationship>> getMockGraph() {
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
            edges.add(new DependenceRelationship(classA, classB, 1));
        }
        return new ImmutablePair<>(new ArrayList<>(classMapping.values()), edges);
    }

    private String getGraphString() {
        try {
            var stream = this.getClass().getClassLoader().getResourceAsStream(MOCK_GRAPH_NAME + ".csv");
            if (stream == null) {
                throw new IllegalStateException("Could not find " + MOCK_GRAPH_NAME + ".csv file");
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
            var objectivesStringBuilder = new StringBuilder("-----------------------------------\n")
                    .append(StringUtils.join(objectivesNames, "  "))
                    .append("\n");
            for (int i = 0; i < objectivesNames.size(); i++) {
                var numberLength = Double.compare(solution.getObjective(i), 0.0) < 0 ? 7 : 6;
                var spacing = " ".repeat(objectivesNames.get(i).length() - numberLength) + "  ";
                objectivesStringBuilder.append(String.format("%.4f%s", solution.getObjective(i), spacing));
            }
            LOGGER.info(objectivesStringBuilder.toString());

            var clustering = variableDecoder.decode(input, EncodingUtils.getInt(solution), encodingType);
            if (clustering.getByClass().size() <= 12) {
                var clusteringStringBuilder = new StringBuilder("Clustering:\n");
                clustering.getByClass().forEach((clazz, cluster) ->
                        clusteringStringBuilder.append(clazz.getHumanReadableName())
                                .append(": ")
                                .append(cluster)
                                .append(", "));
                LOGGER.info(clusteringStringBuilder.toString());
            }
            LOGGER.info("Amount of clusters: {}", clustering.getByCluster().size());
        }
    }
}
