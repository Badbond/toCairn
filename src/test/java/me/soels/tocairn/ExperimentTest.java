package me.soels.tocairn;

import me.soels.tocairn.analysis.sources.SourceAnalysis;
import me.soels.tocairn.analysis.sources.SourceAnalysisInput;
import me.soels.tocairn.model.*;
import me.soels.tocairn.solver.metric.CarvalhoCohesion;
import me.soels.tocairn.solver.metric.CarvalhoCoupling;
import me.soels.tocairn.solver.metric.Metric;
import me.soels.tocairn.solver.moea.ClusteringProblem;
import me.soels.tocairn.solver.moea.EncodingType;
import me.soels.tocairn.solver.moea.VariableDecoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class ExperimentTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTest.class);
    private static final String MOCK_GRAPH_NAME = "simple-graph-2";
    private static final String ZIP_FILE = "project-cleaned.zip";
    private static final String JACOCO_REPORT_FILE = "jacoco.xml";

    @Autowired
    private VariableDecoder variableDecoder;
    @Autowired
    private SourceAnalysis sourceAnalysis;

    @Test
    void runExperimentTestWithMockData() {
        var problemConfig = new MOEAConfiguration();
        problemConfig.setEncodingType(EncodingType.CLUSTER_LABEL);
        var input = prepareMockInput();
        runExperiment(problemConfig, input);
    }

    @Test
    void runExperimentWithSourceCode() throws URISyntaxException, IOException {
        var problemConfig = new MOEAConfiguration();
        problemConfig.setEncodingType(EncodingType.CLUSTER_LABEL);
        var input = getZipInput();
        runExperiment(problemConfig, input);
    }

    private EvaluationInput getZipInput() throws URISyntaxException, IOException {
        var project = tryGetResourceFromDataDir(ZIP_FILE)
                .orElseThrow(() -> new IllegalStateException("Could not find required file " + ZIP_FILE));
        var jacocoXML = tryGetResourceFromDataDir(JACOCO_REPORT_FILE).orElse(null);
        var analysisInput = new SourceAnalysisInput(project, jacocoXML, ".", JAVA_11, null, List.of(".*MainApplication"), null);
        var modelBuilder = new EvaluationInputBuilder(Collections.emptyList());
        var context = sourceAnalysis.prepareContext(modelBuilder, analysisInput);
        sourceAnalysis.analyzeNodes(context);
        sourceAnalysis.analyzeEdges(context);
        var input = modelBuilder.build();
        input.getClasses().stream()
                .filter(clazz -> clazz.getId() == null)
                .forEach(clazz -> clazz.setId(UUID.randomUUID()));
        return input;
    }

    private void runExperiment(MOEAConfiguration config, EvaluationInput input) {
        List<Metric> metrics = List.of(new CarvalhoCoupling(), new CarvalhoCohesion());
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
        //      - Allow equal metric values (same result, different clustering, is still interesting)

        // TODO: As suggested by Carvalho et al., try to disable crossover operators but only allow for mutation
        //  operators. Currently SBX and PM is enabled.
        NondominatedPopulation result = new Executor()
                .withProblem(new ClusteringProblem(metrics, input, config, variableDecoder))
                .withAlgorithm("NSGAII")
                .distributeOnAllCores()
                .withMaxEvaluations(1000000)
                .run();

        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("===================================");
        LOGGER.info("Amount of non-dominated solutions: " + result.size());
        LOGGER.info("Processing took: " + duration + " (H:m:s.millis)");
        printSolutionsData(result, metrics, input, config);
        LOGGER.info("===================================");
    }

    private EvaluationInput prepareMockInput() {
        return new EvaluationInputBuilder(getMockGraph()).build();
    }

    private List<AbstractClass> getMockGraph() {
        var graphLines = Arrays.stream(getGraphString().split("\n"))
                .skip(2)
                .collect(Collectors.toList());
        var classMapping = new LinkedHashMap<String, AbstractClass>();
        for (var line : graphLines) {
            var split = Arrays.stream(line.split(","))
                    .map(StringUtils::trim)
                    .collect(Collectors.toList());
            var classA = classMapping.computeIfAbsent(split.get(0), key ->
                    new OtherClass("Class" + classMapping.size(), split.get(0), null, Collections.emptySet(), 1, false));
            classA.setId(UUID.randomUUID());
            var classB = classMapping.computeIfAbsent(split.get(1), key ->
                    new OtherClass("Class" + classMapping.size(), split.get(1), null, Collections.emptySet(), 1, false));
            classB.setId(UUID.randomUUID());
            classA.getDependenceRelationships().add(new DependenceRelationship(classB, 1, 1L, 1, Collections.emptyMap()));
        }
        return new ArrayList<>(classMapping.values());
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

    private void printSolutionsData(NondominatedPopulation result, List<Metric> metrics, EvaluationInput input, MOEAConfiguration config) {
        var metricNames = metrics.stream()
                .map(metric -> metric.getClass().getSimpleName())
                .collect(Collectors.toList());

        for (var solution : result) {
            var metricsStringBuilder = new StringBuilder("-----------------------------------\n")
                    .append(StringUtils.join(metricNames, "  "))
                    .append("\n");
            for (int i = 0; i < metricNames.size(); i++) {
                var numberLength = Double.compare(solution.getObjective(i), 0.0) < 0 ? 7 : 6;
                var spacing = " ".repeat(metricNames.get(i).length() - numberLength) + "  ";
                metricsStringBuilder.append(String.format("%.4f%s", solution.getObjective(i), spacing));
            }
            LOGGER.info(metricsStringBuilder.toString());

            var clustering = variableDecoder.decode(solution, input, config);
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

    private Optional<Path> tryGetResourceFromDataDir(String fileName) throws URISyntaxException, MalformedURLException {
        var currentDir = this.getClass().getClassLoader().getResource("./");
        var fileLocation = currentDir.toString() + "../../data/" + fileName;
        var url = new URL(fileLocation);
        if (!Files.exists(Path.of(url.toURI()))) {
            fail("Could not find file " + fileLocation);
        }
        if (url != null) {
            return Optional.of(Path.of(url.toURI()));
        }
        return Optional.empty();
    }
}