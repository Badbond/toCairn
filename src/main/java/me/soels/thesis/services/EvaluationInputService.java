package me.soels.thesis.services;

import me.soels.thesis.analysis.dynamic.DynamicAnalysis;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysis;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.thesis.analysis.statik.StaticAnalysis;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;
import me.soels.thesis.model.*;
import me.soels.thesis.repositories.ClassRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
    private final ClassRepository<AbstractClass> allClassRepository;
    private final ClassRepository<OtherClass> otherClassRepository;
    private final ClassRepository<DataClass> dataClassRepository;

    public EvaluationInputService(StaticAnalysis staticAnalysis,
                                  DynamicAnalysis dynamicAnalysis,
                                  EvolutionaryAnalysis evolutionaryAnalysis,
                                  @Qualifier("classRepository") ClassRepository<AbstractClass> allClassRepository,
                                  @Qualifier("classRepository") ClassRepository<OtherClass> otherClassRepository,
                                  @Qualifier("classRepository") ClassRepository<DataClass> dataClassRepository) {
        this.staticAnalysis = staticAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.evolutionaryAnalysis = evolutionaryAnalysis;
        this.allClassRepository = allClassRepository;
        this.otherClassRepository = otherClassRepository;
        this.dataClassRepository = dataClassRepository;
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
     *
     * @param input the input graph to store
     */
    public void storeInput(EvaluationInput input) {
        // TODO: Can we do allClassRepository.saveAll(input.getAllClasses()); ? That would save 2 dependencies.
        otherClassRepository.saveAll(input.getOtherClasses());
        dataClassRepository.saveAll(input.getDataClasses());
    }

    /**
     * Delete the input graph for the given evaluation.
     *
     * @param evaluation the evaluation to delete the input graph for
     */
    public void deleteAllInputs(Evaluation evaluation) {
        allClassRepository.deleteAll(evaluation.getInputs());
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
        }

        var builder = getPopulatedInputBuilder(evaluation);
        staticAnalysis.analyze(builder, analysisInput);
        storeInput(builder.build());
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
        storeInput(builder.build());
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
        storeInput(builder.build());
    }

    /**
     * Returns an {@link EvaluationInputBuilder} with populated data from persistance.
     *
     * @param evaluation the evaluation to create the input builder for
     * @return the populated input builder
     */
    private EvaluationInputBuilder getPopulatedInputBuilder(Evaluation evaluation) {
        return new EvaluationInputBuilder()
                .withClasses(evaluation.getInputs());
    }
}
