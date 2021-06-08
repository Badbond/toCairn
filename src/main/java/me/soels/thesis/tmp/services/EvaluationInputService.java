package me.soels.thesis.tmp.services;

import me.soels.thesis.analysis.dynamic.DynamicAnalysis;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;
import me.soels.thesis.analysis.statik.StaticAnalysis;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.EvaluationInputBuilder;
import me.soels.thesis.tmp.daos.Objective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

import static me.soels.thesis.tmp.daos.Objective.DATA_AUTONOMY;

/**
 * Service responsible for validating whether an {@link EvaluationInput} contains the required information given a
 * set of {@link Objective}. Furthermore, this service has endpoints to construct this input or enhance it based on
 * the analysis types supported in this application.
 */
@Service
public class EvaluationInputService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationInputService.class);
    private final StaticAnalysis staticAnalysis;
    private final DynamicAnalysis dynamicAnalysis;
    private final GraphService graphService;

    public EvaluationInputService(StaticAnalysis staticAnalysis,
                                  DynamicAnalysis dynamicAnalysis,
                                  GraphService graphService) {
        this.staticAnalysis = staticAnalysis;
        this.dynamicAnalysis = dynamicAnalysis;
        this.graphService = graphService;
    }

    /**
     * Checks whether all the input is present for the given objectives.
     *
     * @param evaluationId the id of the evaluation to check whether all input has been provided
     * @param objectives   the objectives to meet input for
     * @return whether all input has been provided given the objectives
     */
    public boolean hasAllRequiredInput(UUID evaluationId, Set<Objective> objectives) {
        var input = graphService.getInput(evaluationId);
        // TODO: Check other objectives' input
        return hasPerformedStaticAnalysis(input) &&
                (!objectives.contains(DATA_AUTONOMY) || hasPerformedDynamicAnalysis(input));
    }

    /**
     * Performs static analysis for the evaluation with the given {@code evaluationId}. The result of analysis
     * will be stored in the database.
     * <p>
     * Regardless of objectives set, static analysis must be executed as the primary graph structure is built from it.
     * The static analysis allows to identify all nodes which need to cluster.
     *
     * @param evaluationId  the evaluation to perform static analysis for
     * @param analysisInput the input required for performing static analysis
     */
    public void performStaticAnalysis(UUID evaluationId, StaticAnalysisInput analysisInput) {
        var evaluationInput = graphService.getInput(evaluationId);
        if (hasPerformedStaticAnalysis(evaluationInput)) {
            throw new IllegalArgumentException("Static analysis already performed.");
        }

        // We always start from static analysis and therefore we get a clean builder
        var builder = new EvaluationInputBuilder(evaluationId);
        staticAnalysis.analyze(builder, analysisInput);
        graphService.storeInput(builder.build());
    }

    private boolean hasPerformedStaticAnalysis(EvaluationInput input) {
        if (input.getAllClasses().isEmpty()) {
            LOGGER.warn("No static analysis was performed as no classes have been recorded.");
            return false;
        }
        return true;
    }

    /**
     * Performs dynamic analysis for the evaluation with the given {@code evaluationId}. The result of analysis
     * will be stored in the database. Prior to dynamic analysis, static analysis has to be performed.
     *
     * @param evaluationId  the evaluation to perform static analysis for
     * @param analysisInput the input required for performing static analysis
     */
    public void performDynamicAnalysis(UUID evaluationId, DynamicAnalysisInput analysisInput) {
        var evaluationInput = graphService.getInput(evaluationId);
        if (hasPerformedDynamicAnalysis(evaluationInput)) {
            throw new IllegalArgumentException("Dynamic analysis already performed.");
        } else if (!hasPerformedStaticAnalysis(evaluationInput)) {
            throw new IllegalArgumentException("Static analysis needs to be performed before dynamic analysis");
        }

        // We always start from static analysis and therefore we get a clean builder
        var builder = new EvaluationInputBuilder(evaluationId);
        dynamicAnalysis.analyze(builder, analysisInput);
        graphService.storeInput(builder.build());
    }

    private boolean hasPerformedDynamicAnalysis(EvaluationInput input) {
        // At least one/several data classes have measured the size
        if (input.getDataClasses().stream().noneMatch(clazz -> clazz.getSize() != null)) {
            LOGGER.warn("No dynamic analysis was performed as no data class was registered with a size");
            return false;
        }
        return true;
    }
}
