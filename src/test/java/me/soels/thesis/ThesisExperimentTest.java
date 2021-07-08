package me.soels.thesis;

import me.soels.thesis.analysis.sources.SourceAnalysis;
import me.soels.thesis.analysis.sources.SourceAnalysisInput;
import me.soels.thesis.clustering.ClusteringProblem;
import me.soels.thesis.clustering.encoding.EncodingType;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.clustering.objectives.CohesionCarvalhoObjective;
import me.soels.thesis.clustering.objectives.CouplingBetweenModuleClassesObjective;
import me.soels.thesis.clustering.objectives.Objective;
import me.soels.thesis.model.*;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class ThesisExperimentTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThesisExperimentTest.class);
    private static final String MOCK_GRAPH_NAME = "simple-graph-2";
    private static final String ZIP_FILE = "thesis-project-master.zip";
    private static final String JACOCO_REPORT_FILE = "jacoco.xml";

    @Autowired
    private VariableDecoder variableDecoder;
    @Autowired
    private SourceAnalysis sourceAnalysis;

    @Test
    void runExperimentTestWithMockData() {
        var problemConfig = new EvaluationConfiguration();
        problemConfig.setEncodingType(EncodingType.CLUSTER_LABEL);
        var input = prepareMockInput();
        runExperiment(problemConfig, input);
    }

    @Test
    void runExperimentWithSourceCode() throws URISyntaxException, IOException {
        var problemConfig = new EvaluationConfiguration();
        problemConfig.setEncodingType(EncodingType.CLUSTER_LABEL);
        var input = getZipInput();
        runExperiment(problemConfig, input);
    }

    private EvaluationInput getZipInput() throws URISyntaxException, IOException {
        var project = tryGetResource(ZIP_FILE)
                .orElseThrow(() -> new IllegalStateException("Could not find required file " + ZIP_FILE));
        var jacocoXML = tryGetResource(JACOCO_REPORT_FILE).orElse(null);
        var analysisInput = new SourceAnalysisInput(project, jacocoXML, JAVA_11, null);
        var modelBuilder = new EvaluationInputBuilder(Collections.emptyList());
        var context = sourceAnalysis.prepareContext(modelBuilder, analysisInput);
        sourceAnalysis.analyzeEdges(context);
        sourceAnalysis.analyzeEdges(context);
        return modelBuilder.build();
    }

    private void runExperiment(EvaluationConfiguration config, EvaluationInput input) {
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
                .withProblem(new ClusteringProblem(objectives, input, config, variableDecoder))
                .withAlgorithm("NSGAII")
                .distributeOnAllCores()
                .withMaxEvaluations(1000000)
                .run();

        // Display the results
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("===================================");
        LOGGER.info("Amount of non-dominated solutions: " + result.size());
        LOGGER.info("Processing took: " + duration + " (H:m:s.millis)");
        printSolutionsData(result, objectives, input, config);
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
            var classA = classMapping.computeIfAbsent(split.get(0), key -> new OtherClass("Class" + classMapping.size(), split.get(0)));
            var classB = classMapping.computeIfAbsent(split.get(1), key -> new OtherClass("Class" + classMapping.size(), split.get(1)));
            classA.getDependenceRelationships().add(new DependenceRelationship(classB, 1));
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

    private void printSolutionsData(NondominatedPopulation result, List<Objective> objectives, EvaluationInput input, EvaluationConfiguration config) {
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

    private Optional<Path> tryGetResource(String resource) throws URISyntaxException {
        var url = this.getClass().getClassLoader().getResource(resource);
        if (url != null) {
            return Optional.of(Path.of(url.toURI()));
        }
        return Optional.empty();
    }
}
