package me.soels.thesis.clustering;

import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.clustering.objectives.*;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationConfiguration;
import me.soels.thesis.model.EvaluationInput;
import org.moeaframework.Executor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class that runs a clustering algorithm based on the provided {@link Evaluation} and
 * {@link EvaluationConfiguration}.
 */
@Service
public class ClusteringExecutorProvider {
    protected final VariableDecoder variableDecoder;

    public ClusteringExecutorProvider(VariableDecoder variableDecoder) {
        this.variableDecoder = variableDecoder;
    }

    public Executor getExecutor(Evaluation evaluation, EvaluationInput input) {
        var configuration = evaluation.getConfiguration();
        var objectives = evaluation.getObjectives().stream()
                .flatMap(objectiveType -> getMetricsForObjective(objectiveType).stream())
                .collect(Collectors.toUnmodifiableList());
        // TODO: The problem is closable: I should close it.
        var problem = new ClusteringProblem(List.copyOf(objectives), input, evaluation.getConfiguration(), variableDecoder);
//         TODO: Add analyzer:
//          Analyzer analyzer = new Analyzer()
//                  .withProblem(problem)
//                  .includeAllMetrics()
//                  .showAll()
//                  .add(configuration.getAlgorithm().toString(), null);
        return new Executor()
                .distributeOnAllCores()
                .withProblem(problem)
                .withMaxTime(configuration.getMaxTime().orElse(-1L))
                .withMaxEvaluations(configuration.getMaxEvaluations())
                .withAlgorithm(configuration.getAlgorithm().toString());
        // TODO: Configure operators: disable crossover, keep mutation, add enhanced mutation of moving multiple
        //  connected nodes.
    }

    public List<Objective> getMetricsForObjective(ObjectiveType objectiveType) {
        switch (objectiveType) {
            case ONE_PURPOSE:
                // TODO: We probably want to have only one metric per objective such that we can have one number
                //  indicate the quality of the solution based on that characteristic instead of having multiple.
                //  Perhaps we can sum these objectives. However, they should have equal ranges otherwise 0-10 would
                //  overtake 0-1 in almost all cases. Thus one_purpose=(cohesion+coupling)/2 should range from (0-1).
                return List.of(new CohesionCarvalhoObjective(), new CouplingCarvalhoObjective());
            case DATA_AUTONOMY:
                return List.of(new TemporaryDataAutonomyMetric());
            case BOUNDED_CONTEXT:
                return List.of(new TemporaryBoundedContextMetric());
            case SHARED_DEVELOPMENT_LIFECYCLE:
                return List.of(new TemporarySharedLifecycleMetric());
            default:
                throw new IllegalStateException("Unsupported objective type found " + objectiveType);
        }
    }
}
