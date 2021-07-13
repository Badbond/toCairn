package me.soels.thesis.clustering;

import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.clustering.objectives.*;
import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationConfiguration;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.EvolutionaryAlgorithm;
import org.moeaframework.Analyzer;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.spi.ProblemFactory;
import org.moeaframework.core.spi.ProblemProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class that provides {@link Problem}, {@link Executor} and {@link Analyzer} instances based on given
 * {@link EvaluationConfiguration} configuration and {@link EvaluationInput} input data.
 */
@Service
public class ClusteringContextProvider {
    protected final VariableDecoder variableDecoder;

    public ClusteringContextProvider(VariableDecoder variableDecoder) {
        this.variableDecoder = variableDecoder;
    }

    public Problem createProblem(Evaluation evaluation, EvaluationInput input) {
        var objectives = evaluation.getObjectives().stream()
                .flatMap(objectiveType -> getMetricsForObjective(objectiveType).stream())
                .collect(Collectors.toUnmodifiableList());
        return new ClusteringProblem(List.copyOf(objectives), input, evaluation.getConfiguration(), variableDecoder);
    }

    public Analyzer createAnalyzer(NondominatedPopulation result, Executor executor, EvolutionaryAlgorithm algorithm) {
        return new Analyzer()
                .withSameProblemAs(executor)
                .add(algorithm.toString(), result)
                .includeAllMetrics()
                .showAll();
    }

    public ProblemFactory createProblemProvider(Evaluation evaluation, EvaluationInput input) {
        var objectives = evaluation.getObjectives().stream()
                .flatMap(objectiveType -> getMetricsForObjective(objectiveType).stream())
                .collect(Collectors.toUnmodifiableList());
        var problem = new ClusteringProblem(List.copyOf(objectives), input, evaluation.getConfiguration(), variableDecoder);
        var factory = new ProblemFactory();
        factory.addProvider(new ProblemProvider() {
            @Override
            public Problem getProblem(String name) {
                if ("MOEAC".equals(name)) {
                    return problem;
                }
                return null;
            }

            @Override
            public NondominatedPopulation getReferenceSet(String name) {
                return null;
            }
        });
        return factory;
    }

    public Executor createExecutor(Problem problem, EvaluationConfiguration configuration) {
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
