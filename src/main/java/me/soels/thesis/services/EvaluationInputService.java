package me.soels.thesis.services;

import me.soels.thesis.analysis.dynamic.DynamicAnalysis;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysis;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysisInput;
import me.soels.thesis.analysis.statik.StaticAnalysis;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;
import me.soels.thesis.clustering.objectives.ObjectiveType;
import me.soels.thesis.model.*;
import me.soels.thesis.repositories.ClassRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;

import static me.soels.thesis.clustering.objectives.ObjectiveType.DATA_AUTONOMY;
import static me.soels.thesis.clustering.objectives.ObjectiveType.SHARED_DEVELOPMENT_LIFECYCLE;
import static me.soels.thesis.model.AnalysisType.*;

/**
 * Service responsible for validating whether an {@link EvaluationInput} contains the required information given a
 * set of {@link ObjectiveType}. Furthermore, this service has endpoints to construct this input or enhance it based on
 * the analysis types supported in this application.
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

    public EvaluationInput getInput(Evaluation evaluation) {
        var classes = evaluation.getInputs();
        return new EvaluationInputBuilder()
                .withClasses(classes)
                .build();
    }

    public void storeInput(EvaluationInput input) {
        otherClassRepository.saveAll(input.getOtherClasses());
        dataClassRepository.saveAll(input.getDataClasses());
    }

    public void deleteAllInputs(Evaluation evaluation) {
        allClassRepository.deleteAll(evaluation.getInputs());
    }

    /**
     * Checks whether all the input is present for the given objectives.
     * <p>
     * Note that {@link AnalysisType#STATIC} analysis is always required as this generates a complete graph of classes
     * to cluster. As {@link ObjectiveType#ONE_PURPOSE} and {@link ObjectiveType#BOUNDED_CONTEXT} only rely on the
     * result from static analysis, we do not check them explicitly.
     *
     * @param evaluation the evaluation to check whether all input has been provided
     * @param objectives the objectives to meet input for
     * @return whether all input has been provided given the objectives
     */
    public boolean hasAllRequiredInput(Evaluation evaluation, Set<ObjectiveType> objectives) {
        return hasPerformedAnalysis(evaluation, STATIC) &&
                (!objectives.contains(DATA_AUTONOMY) || hasPerformedAnalysis(evaluation, DYNAMIC)) &&
                (!objectives.contains(SHARED_DEVELOPMENT_LIFECYCLE) || hasPerformedAnalysis(evaluation, EVOLUTIONARY));
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
        if (hasPerformedAnalysis(evaluation, STATIC)) {
            throw new IllegalArgumentException("Static analysis already performed.");
        }

        // We always start from static analysis and therefore we get a clean builder
        var builder = new EvaluationInputBuilder();
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
        var evaluationInput = getInput(evaluation);
        if (hasPerformedAnalysis(evaluation, DYNAMIC)) {
            throw new IllegalArgumentException("Dynamic analysis already performed.");
        } else if (!hasPerformedAnalysis(evaluation, STATIC)) {
            throw new IllegalArgumentException("Static analysis needs to be performed before dynamic analysis");
        }

        var builder = new EvaluationInputBuilder(evaluationInput);
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
    private void performEvolutionaryAnalysis(Evaluation evaluation, EvolutionaryAnalysisInput analysisInput) {
        var evaluationInput = getInput(evaluation);
        if (hasPerformedAnalysis(evaluation, EVOLUTIONARY)) {
            throw new IllegalArgumentException("Evolutionary analysis already performed.");
        } else if (!hasPerformedAnalysis(evaluation, STATIC)) {
            throw new IllegalArgumentException("Static analysis needs to be performed before evolutionary analysis");
        }

        var builder = new EvaluationInputBuilder(evaluationInput);
        evolutionaryAnalysis.analyze(builder, analysisInput);
        storeInput(builder.build());
    }

    /**
     * Returns whether the expected analysis has been performed for the given evaluation.
     *
     * @param evaluation       the evaluation to validate
     * @param expectedAnalysis the analysis to expect
     * @return whether the expected analysis has been performed
     */
    private boolean hasPerformedAnalysis(Evaluation evaluation, AnalysisType expectedAnalysis) {
        return evaluation.getExecutedAnalysis().contains(expectedAnalysis);
    }
}
