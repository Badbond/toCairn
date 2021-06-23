package me.soels.thesis.services;

import me.soels.thesis.analysis.dynamic.DynamicAnalysis;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysis;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.thesis.analysis.statik.StaticAnalysis;
import me.soels.thesis.analysis.statik.StaticAnalysisContext;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.EvaluationInputBuilder;
import me.soels.thesis.repositories.ClassRepository;
import me.soels.thesis.repositories.EvaluationRepository;
import me.soels.thesis.repositories.OtherClassRepository;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static me.soels.thesis.model.AnalysisType.*;

/**
 * Service responsible for maintaining the {@link EvaluationInput} graph. This service allows for commanding certain
 * analyses to be executed and stores the result of those analyses to the database.
 */
@Service
public class EvaluationInputService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationInputService.class);
    private final StaticAnalysis staticAnalysis;
    private final DynamicAnalysis dynamicAnalysis;
    private final EvolutionaryAnalysis evolutionaryAnalysis;
    private final ClassRepository<AbstractClass> classRepository;
    private final OtherClassRepository otherClassRepository;
    private final EvaluationRepository evaluationRepository;
    private final AtomicBoolean staticAnalysisRunning = new AtomicBoolean(false);

    public EvaluationInputService(StaticAnalysis staticAnalysis,
                                  DynamicAnalysis dynamicAnalysis,
                                  EvolutionaryAnalysis evolutionaryAnalysis,
                                  @Qualifier("classRepository") ClassRepository<AbstractClass> classRepository,
                                  @Qualifier("otherClassRepository") OtherClassRepository otherClassRepository,
                                  EvaluationRepository evaluationRepository) {
        this.staticAnalysis = staticAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.evolutionaryAnalysis = evolutionaryAnalysis;
        this.classRepository = classRepository;
        this.otherClassRepository = otherClassRepository;
        this.evaluationRepository = evaluationRepository;
    }

    /**
     * Store the given input graph.
     * <p>
     * The {@link Evaluation} needs to be given as that contains the outgoing dependency
     * to the input graph nodes.
     *
     * @param evaluation the evaluation to set the inputs for
     * @param input      the input graph to store
     */
    public void storeInput(Evaluation evaluation, EvaluationInput input) {
        evaluation.setInputs(input.getClasses());
        // This also saves/created the nodes in the graph
        evaluationRepository.save(evaluation);
    }

    /**
     * Delete the input graph for the given evaluation.
     *
     * @param evaluation the evaluation to delete the input graph for
     */
    public void deleteAllInputs(Evaluation evaluation) {
        classRepository.deleteAll(evaluation.getInputs());
    }

    /**
     * Performs static analysis for the given evaluation. The result of analysis will be stored in the database.
     * <p>
     * Regardless of objectives set, static analysis must be executed as the primary graph structure is built from it.
     * Static analysis needs to be executed first such that other analyses can enhance the graph constructed from it.
     * The static analysis allows to identify all nodes which need to cluster.
     *
     * @param evaluation    the evaluation to perform static analysis for
     * @param analysisInput the input required for performing static analysis
     */
    public void performStaticAnalysis(Evaluation evaluation, StaticAnalysisInput analysisInput) {
        if (evaluation.getExecutedAnalysis().contains(STATIC)) {
            throw new IllegalArgumentException("Static analysis already performed.");
        } else if (!staticAnalysisRunning.compareAndSet(false, true)) {
            throw new IllegalArgumentException("An static analysis is already running. The JavaParser library has " +
                    "degraded coverage due to errors when running in parallel. Stopping analysis");
        }

        try {
            var start = System.currentTimeMillis();
            var builder = getPopulatedInputBuilder(evaluation);
            var context = staticAnalysis.prepareContext(builder, analysisInput);
            // We split the extraction and persistence of nodes and edges as we can then more efficiently create the
            // relationships based on existent nodes with generated IDs.
            extractNodes(evaluation, builder, context);
            extractEdges(evaluation, context);
            var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
            LOGGER.info("Total static analysis took {} (H:m:s.millis)", duration);
        } finally {
            // Always reset the lock
            staticAnalysisRunning.set(false);
        }
    }

    private void extractEdges(Evaluation evaluation, StaticAnalysisContext context) {
        // Construct builder again from persisted state now that we have nodes with generated IDs
        var builder = getPopulatedInputBuilder(evaluation);
        staticAnalysis.analyzeEdges(context);

        LOGGER.info("Storing relationships");
        builder.getDataClasses().forEach(dataClass -> dataClass.getDependenceRelationships()
                .forEach(rel -> classRepository.addDependencyRelationship(dataClass.getId(), rel.getCallee().getId(), rel)));
        builder.getOtherClasses().forEach(otherClass -> {
            otherClass.getDependenceRelationships().forEach(rel -> classRepository.addDependencyRelationship(otherClass.getId(), rel.getCallee().getId(), rel));
            otherClass.getDataRelationships().forEach(rel -> otherClassRepository.addDataRelationship(otherClass.getId(), rel.getCallee().getId(), rel));
        });
        LOGGER.info("Stored relationships");
    }

    private void extractNodes(Evaluation evaluation, EvaluationInputBuilder builder, StaticAnalysisContext context) {
        staticAnalysis.analyzeNodes(context);
        LOGGER.info("Storing nodes");
        storeInput(evaluation, builder.build());
        LOGGER.info("Stored {} nodes", evaluation.getInputs().size());
    }

    /**
     * Performs dynamic analysis for the given evaluation. The result of analysis will be stored in the database.
     * Prior to dynamic analysis, static analysis has to be performed.
     *
     * @param evaluation    the evaluation to perform dynamic analysis for
     * @param analysisInput the input required for performing dynamic analysis
     */
    public void performDynamicAnalysis(Evaluation evaluation, DynamicAnalysisInput analysisInput) {
        if (evaluation.getExecutedAnalysis().contains(DYNAMIC)) {
            throw new IllegalArgumentException("Dynamic analysis already performed.");
        } else if (!evaluation.getExecutedAnalysis().contains(STATIC)) {
            throw new IllegalArgumentException("Static analysis needs to be performed before dynamic analysis");
        }

        var builder = getPopulatedInputBuilder(evaluation);
        dynamicAnalysis.analyze(builder, analysisInput);
        storeInput(evaluation, builder.build());
    }

    /**
     * Performs evolutionary analysis for the given evaluation. The result of analysis will be stored in the database.
     * Prior to evolutionary analysis, static analysis has to be performed.
     *
     * @param evaluation    the evaluation to perform evolutionary analysis for
     * @param analysisInput the input required for performing evolutionary analysis
     */
    public void performEvolutionaryAnalysis(Evaluation evaluation, EvolutionaryAnalysisInput analysisInput) {
        if (evaluation.getExecutedAnalysis().contains(EVOLUTIONARY)) {
            throw new IllegalArgumentException("Evolutionary analysis already performed.");
        } else if (!evaluation.getExecutedAnalysis().contains(STATIC)) {
            throw new IllegalArgumentException("Static analysis needs to be performed before evolutionary analysis");
        }

        var builder = getPopulatedInputBuilder(evaluation);
        evolutionaryAnalysis.analyze(builder, analysisInput);
        storeInput(evaluation, builder.build());
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
