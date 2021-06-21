package me.soels.thesis.services;

import me.soels.thesis.analysis.dynamic.DynamicAnalysis;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysis;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.thesis.analysis.statik.StaticAnalysis;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.EvaluationInputBuilder;
import me.soels.thesis.repositories.ClassRepository;
import me.soels.thesis.repositories.EvaluationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static me.soels.thesis.model.AnalysisType.*;

/**
 * Service responsible for maintaining the {@link EvaluationInput} graph. This service allows for commanding certain
 * analyses to be executed and stores the result of those analyses to the database.
 */
@Service
public class EvaluationInputService {
    private final StaticAnalysis staticAnalysis;
    private final DynamicAnalysis dynamicAnalysis;
    private final EvolutionaryAnalysis evolutionaryAnalysis;
    private final ClassRepository<AbstractClass> classRepository;
    private final EvaluationRepository evaluationRepository;
    private final AtomicBoolean staticAnalysisRunning = new AtomicBoolean(false);

    public EvaluationInputService(StaticAnalysis staticAnalysis,
                                  DynamicAnalysis dynamicAnalysis,
                                  EvolutionaryAnalysis evolutionaryAnalysis,
                                  @Qualifier("classRepository") ClassRepository<AbstractClass> classRepository,
                                  EvaluationRepository evaluationRepository) {
        this.staticAnalysis = staticAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.evolutionaryAnalysis = evolutionaryAnalysis;
        this.classRepository = classRepository;
        this.evaluationRepository = evaluationRepository;
    }

    /**
     * Retrieves the stored input graph for the given evaluation.
     *
     * @param evaluation the evaluation to retrieve the input graph for
     * @return the stored input graph
     */
    public EvaluationInput getInput(Evaluation evaluation) {
        return getPopulatedInputBuilder(evaluation).build();
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
            var builder = getPopulatedInputBuilder(evaluation);
            staticAnalysis.analyze(builder, analysisInput);
            storeInput(evaluation, builder.build());
        } finally {
            // Always reset the lock
            staticAnalysisRunning.set(false);
        }
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
     * Returns an {@link EvaluationInputBuilder} with populated data from persistance.
     *
     * @param evaluation the evaluation to create the input builder for
     * @return the populated input builder
     */
    private EvaluationInputBuilder getPopulatedInputBuilder(Evaluation evaluation) {
        var classes = evaluation.getInputs();
        var builder = new EvaluationInputBuilder();
        return builder.withClasses(classes)
                .withDependencies(classes.stream()
                        .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                        .collect(Collectors.toList()))
                .withDataRelationships(builder.getOtherClasses().stream()
                        .flatMap(clazz -> clazz.getDataRelationships().stream())
                        .collect(Collectors.toList()));
    }
}
