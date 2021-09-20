package me.soels.tocairn.services;

import me.soels.tocairn.analysis.dynamic.DynamicAnalysis;
import me.soels.tocairn.analysis.dynamic.DynamicAnalysisInput;
import me.soels.tocairn.analysis.evolutionary.EvolutionaryAnalysis;
import me.soels.tocairn.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.tocairn.analysis.sources.SourceAnalysis;
import me.soels.tocairn.analysis.sources.SourceAnalysisContext;
import me.soels.tocairn.analysis.sources.SourceAnalysisInput;
import me.soels.tocairn.model.*;
import me.soels.tocairn.repositories.ClassRepository;
import me.soels.tocairn.repositories.EvaluationRepository;
import me.soels.tocairn.repositories.OtherClassRepository;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static me.soels.tocairn.model.AnalysisType.*;

/**
 * Service responsible for maintaining the {@link EvaluationInput} graph. This service allows for commanding certain
 * analyses to be executed and stores the result of those analyses to the database.
 */
@Service
public class EvaluationInputService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationInputService.class);
    private final SourceAnalysis sourceAnalysis;
    private final EvolutionaryAnalysis evolutionaryAnalysis;
    private final DynamicAnalysis dynamicAnalysis;
    private final EvaluationRepository evaluationRepository;
    private final ClassRepository<AbstractClass> classRepository;
    private final OtherClassRepository otherClassRepository;
    private final AtomicBoolean sourceAnalysisRunning = new AtomicBoolean(false);
    private final Neo4jClient client;

    public EvaluationInputService(SourceAnalysis sourceAnalysis,
                                  EvolutionaryAnalysis evolutionaryAnalysis,
                                  DynamicAnalysis dynamicAnalysis,
                                  EvaluationRepository evaluationRepository,
                                  @Qualifier("classRepository") ClassRepository<AbstractClass> classRepository,
                                  @Qualifier("otherClassRepository") OtherClassRepository otherClassRepository,
                                  Neo4jClient client) {
        this.sourceAnalysis = sourceAnalysis;
        this.evolutionaryAnalysis = evolutionaryAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.evaluationRepository = evaluationRepository;
        this.classRepository = classRepository;
        this.otherClassRepository = otherClassRepository;
        this.client = client;
    }

    /**
     * Retrieves the input graph from the database and sets it in the given evaluation instance.
     * <p>
     * Here, we use a custom query using the {@link Neo4jClient} and deserialize the response. This was necessary
     * for two reasons. Firstly, using OEM caused session to hang due to much model checking caused by many cycles in
     * relationships. Secondly, custom queries caused the ORM to not deserialize all relationships, resulting in
     * incomplete data.
     *
     * @param evaluation the evaluation to set the input graph for
     */
    public void populateInputFromDb(Evaluation evaluation) {
        var nodeList = classRepository.getInputNodesWithoutRel(evaluation.getId()).stream()
                .collect(Collectors.toMap(AbstractClass::getId, clazz -> clazz));
        var query = client.query("MATCH (c :AbstractClass { evaluationId: '" + evaluation.getId() + "' }) " +
                "OPTIONAL MATCH (c)-[interactsWith :InteractsWith]->(interactTarget :AbstractClass) " +
                "OPTIONAL MATCH (c)-[dataDepends :DataDepends]->(dataTarget :DataClass) " +
                "RETURN c, collect(interactsWith), collect(dataDepends), collect(interactTarget), collect(dataTarget)");
        var queryResult = query.fetch().all();
        var nodes = queryResult.stream()
                .map(node -> {
                    var c = (InternalNode) node.get("c");
                    return Pair.of(c.id(), UUID.fromString(c.get("id").asString()));
                })
                .collect(Collectors.toMap(Pair::getKey, pair -> nodeList.get(pair.getValue())));

        for (var result : queryResult) {
            var caller = nodes.get(((InternalNode) result.get("c")).id());
            convertDataDeps(caller, nodes, getRelationship(result, "dataDepends"));
            convertInteractsWith(caller, nodes, getRelationship(result, "interactsWith"));
        }
        evaluation.setInputs(new ArrayList<>(nodes.values()));
    }

    @SuppressWarnings("unchecked") // Safe cast due to modelled relationship and query
    private List<InternalRelationship> getRelationship(Map<String, Object> result, String relationship) {
        return (List<InternalRelationship>) result.get("collect(" + relationship + ")");
    }

    private void convertDataDeps(AbstractClass caller, Map<Long, AbstractClass> nodes, List<InternalRelationship> dataDeps) {
        for (var rel : dataDeps) {
            if (((OtherClass) caller).getDataRelationships().stream().anyMatch(existing -> existing.getId().equals(rel.id()))) {
                // Relationship already exists, continuing.
                continue;
            }
            var callee = (DataClass) nodes.get(rel.endNodeId());
            var type = DataRelationshipType.valueOf(rel.get("type").asString());
            var staticFreq = rel.get("staticFrequency").asInt();
            var dynamicFreq = rel.get("dynamicFrequency").asLong();
            var connections = rel.get("connections").asInt();
            var sharedClasses = new HashMap<String, Long>(); // TODO: Shared classes.
            var dao = new DataRelationship(callee, type, staticFreq, dynamicFreq, connections, sharedClasses);
            dao.setId(rel.id());
            // Safe to cast, there is a data relationship which is only possible from other classes.
            ((OtherClass) caller).getDataRelationships().add(dao);
        }
    }

    private void convertInteractsWith(AbstractClass caller, Map<Long, AbstractClass> nodes, List<InternalRelationship> interactsWith) {
        for (var rel : interactsWith) {
            if (caller.getDependenceRelationships().stream().anyMatch(existing -> existing.getId().equals(rel.id()))) {
                // Relationship already exists on caller, continuing.
                continue;
            }
            var callee = nodes.get(rel.endNodeId());
            var staticFreq = rel.get("staticFrequency").asInt();
            var dynamicFreq = rel.get("dynamicFrequency").asLong();
            var connections = rel.get("connections").asInt();
            var sharedClasses = new HashMap<String, Long>(); // TODO: Shared classes.
            var dao = new DependenceRelationship(callee, staticFreq, dynamicFreq, connections, sharedClasses);
            dao.setId(rel.id());
            caller.getDependenceRelationships().add(dao);
        }
    }

    /**
     * Performs source analysis for the given evaluation. The result of analysis will be stored in the database.
     * <p>
     * Regardless of objectives set, source analysis must be executed as the primary graph structure is built from it.
     * Source analysis needs to be executed first such that other analyses can enhance the graph constructed from it.
     * The source analysis allows to identify all nodes which need to cluster.
     *
     * @param evaluation    the evaluation to perform source analysis for
     * @param analysisInput the input required for performing source analysis
     */
    public void performSourceAnalysis(Evaluation evaluation, SourceAnalysisInput analysisInput) throws IOException {
        if (evaluation.getExecutedAnalysis().contains(SOURCE)) {
            throw new IllegalArgumentException("Source analysis already performed.");
        } else if (!sourceAnalysisRunning.compareAndSet(false, true)) {
            throw new IllegalArgumentException("A source analysis is already running. The JavaParser library has " +
                    "degraded coverage due to errors when running in parallel. Stopping analysis");
        }

        try {
            var start = System.currentTimeMillis();
            LOGGER.info("Starting source analysis on {}", analysisInput.getPathToZip());
            var builder = new EvaluationInputBuilder(evaluation.getInputs());
            var context = sourceAnalysis.prepareContext(builder, analysisInput);
            // We split the extraction and persistence of nodes and edges as we can then more efficiently create the
            // relationships based on existent nodes with generated IDs.
            extractNodes(evaluation, context);
            extractEdges(evaluation, context);
            var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
            LOGGER.info("Total source analysis took {} (H:m:s.millis)", duration);
        } finally {
            // Always reset the lock
            sourceAnalysisRunning.set(false);
        }
    }

    private void extractNodes(Evaluation evaluation, SourceAnalysisContext context) {
        sourceAnalysis.analyzeNodes(context);

        LOGGER.info("Storing nodes");
        evaluation.setInputs(context.getResultBuilder().build().getClasses());

        // Store the evaluation ID on every class as ORM queries have proven to be troublesome. This way, we can more
        // efficiently query the DB.
        var evaluationId = evaluation.getId();
        evaluation.getInputs().forEach(clazz -> clazz.setEvaluationId(evaluationId));

        // This has to be done with extreme care since it tries to model check the Java state with that in the DB.
        // ONLY in this case it is fine as we don't have the edges extracted yet.
        evaluation = evaluationRepository.save(evaluation);
        LOGGER.info("Stored {} nodes", evaluation.getInputs().size());
    }

    private void extractEdges(Evaluation evaluation, SourceAnalysisContext context) {
        // Construct builder again from persisted state now that we have nodes with generated IDs
        var builder = new EvaluationInputBuilder(evaluation.getInputs());
        sourceAnalysis.analyzeEdges(context);

        LOGGER.info("Storing relationships");
        builder.getDataClasses().forEach(dataClass -> dataClass.getDependenceRelationships()
                .forEach(rel -> classRepository.addDependencyRelationship(dataClass.getId(), rel.getCallee().getId(), rel)));
        builder.getOtherClasses().forEach(otherClass -> {
            otherClass.getDependenceRelationships().forEach(rel -> classRepository.addDependencyRelationship(otherClass.getId(), rel.getCallee().getId(), rel));
            otherClass.getDataRelationships().forEach(rel -> otherClassRepository.addDataRelationship(otherClass.getId(), rel.getCallee().getId(), rel));
        });
        LOGGER.info("Stored relationships");
    }

    /**
     * Performs dynamic analysis for the given evaluation. The result of analysis will be stored in the database.
     *
     * @param evaluation    the evaluation to perform source analysis for
     * @param analysisInput the input required for performing source analysis
     */
    public void performDynamicAnalysis(Evaluation evaluation, DynamicAnalysisInput analysisInput) throws IOException {
        if (!evaluation.getExecutedAnalysis().contains(SOURCE)) {
            throw new IllegalArgumentException("Source analysis must be performed first.");
        } else if (evaluation.getExecutedAnalysis().contains(DYNAMIC)) {
            throw new IllegalArgumentException("Dynamic analysis already performed.");
        }

        var start = System.currentTimeMillis();
        LOGGER.info("Starting dynamic analysis on {}", analysisInput.getPathToJfrFile());
        var builder = new EvaluationInputBuilder(evaluation.getInputs());
        dynamicAnalysis.analyze(builder, analysisInput);

        // Persist the size data
        builder.getClasses().forEach(clazz -> classRepository.setSize(clazz.getId(), clazz.getSize()));

        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Dynamic analysis took {} (H:m:s.millis)", duration);
    }

    /**
     * Performs evolutionary analysis for the given evaluation. The result of analysis will be stored in the database.
     * Prior to evolutionary analysis, source analysis has to be performed.
     *
     * @param evaluation    the evaluation to perform evolutionary analysis for
     * @param analysisInput the input required for performing evolutionary analysis
     */
    public void performEvolutionaryAnalysis(Evaluation evaluation, EvolutionaryAnalysisInput analysisInput) {
        if (evaluation.getExecutedAnalysis().contains(EVOLUTIONARY)) {
            throw new IllegalArgumentException("Evolutionary analysis already performed.");
        } else if (!evaluation.getExecutedAnalysis().contains(SOURCE)) {
            throw new IllegalArgumentException("Source analysis needs to be performed before evolutionary analysis");
        }

        var builder = new EvaluationInputBuilder(evaluation.getInputs());
        evolutionaryAnalysis.analyze(builder, analysisInput);
        // TODO: Create custom query for persisting evoluationary relationships: persistInputRelationships(evaluation, builder.build());
    }
}
