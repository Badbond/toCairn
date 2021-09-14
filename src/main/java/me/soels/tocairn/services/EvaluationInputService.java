package me.soels.tocairn.services;

import me.soels.tocairn.analysis.dynamic.DynamicAnalysis;
import me.soels.tocairn.analysis.dynamic.DynamicAnalysisInput;
import me.soels.tocairn.analysis.evolutionary.EvolutionaryAnalysis;
import me.soels.tocairn.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.tocairn.analysis.sources.SourceAnalysis;
import me.soels.tocairn.analysis.sources.SourceAnalysisContext;
import me.soels.tocairn.analysis.sources.SourceAnalysisInput;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.EvaluationInputBuilder;
import me.soels.tocairn.repositories.ClassRepository;
import me.soels.tocairn.repositories.OtherClassRepository;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final EvaluationService evaluationService;
    private final ClassRepository<AbstractClass> classRepository;
    private final OtherClassRepository otherClassRepository;
    private final AtomicBoolean sourceAnalysisRunning = new AtomicBoolean(false);

    public EvaluationInputService(SourceAnalysis sourceAnalysis,
                                  EvolutionaryAnalysis evolutionaryAnalysis,
                                  DynamicAnalysis dynamicAnalysis,
                                  EvaluationService evaluationService,
                                  @Qualifier("classRepository") ClassRepository<AbstractClass> classRepository,
                                  @Qualifier("otherClassRepository") OtherClassRepository otherClassRepository) {
        this.sourceAnalysis = sourceAnalysis;
        this.evolutionaryAnalysis = evolutionaryAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.evaluationService = evaluationService;
        this.classRepository = classRepository;
        this.otherClassRepository = otherClassRepository;
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
            var builder = getPopulatedInputBuilder(evaluation);
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
        // This has to be done with extreme care since it tries to model check the Java state with that in the DB.
        // ONLY in this case it is fine as we don't have the edges extracted yet.
        evaluation = evaluationService.saveTotal(evaluation);
        LOGGER.info("Stored {} nodes", evaluation.getInputs().size());
    }

    private void extractEdges(Evaluation evaluation, SourceAnalysisContext context) {
        // Construct builder again from persisted state now that we have nodes with generated IDs
        var builder = getPopulatedInputBuilder(evaluation);
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
        var builder = getPopulatedInputBuilder(evaluation);
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

        var builder = getPopulatedInputBuilder(evaluation);
        evolutionaryAnalysis.analyze(builder, analysisInput);
        // TODO: Create custom query for persisting evoluationary relationships: persistInputRelationships(evaluation, builder.build());
    }

    /**
     * Returns an {@link EvaluationInputBuilder} with populated data from persistence.
     *
     * @param evaluation the evaluation to create the input builder for
     * @return the populated input builder
     */
    private EvaluationInputBuilder getPopulatedInputBuilder(Evaluation evaluation) {
        var classes = evaluation.getInputs();
        return new EvaluationInputBuilder(classes);
    }
}
