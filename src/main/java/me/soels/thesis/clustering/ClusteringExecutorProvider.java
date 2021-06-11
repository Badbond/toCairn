package me.soels.thesis.clustering;

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
    public Executor getExecutor(Evaluation evaluation, EvaluationInput input) {
        var configuration = evaluation.getConfiguration();
        var objectives = evaluation.getObjectives().stream()
                .flatMap(objectiveType -> getMetricsForObjective(objectiveType).stream())
                .collect(Collectors.toUnmodifiableList());
        return new Executor()
                .distributeOnAllCores()
                .withProblem(new ClusteringProblem(List.copyOf(objectives), input, evaluation.getConfiguration()))
                .withMaxTime(configuration.getMaxTime().orElse(-1L))
                .withMaxEvaluations(configuration.getMaxEvaluations())
                .withAlgorithm(configuration.getAlgorithm().toString());
        // TODO: Configure operators
    }

    private List<Objective> getMetricsForObjective(ObjectiveType objectiveType) {
        switch (objectiveType) {
            case ONE_PURPOSE:
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
