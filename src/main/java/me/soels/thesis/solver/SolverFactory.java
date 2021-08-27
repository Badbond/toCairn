package me.soels.thesis.solver;

import me.soels.thesis.model.Evaluation;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.HierarchicalConfiguration;
import me.soels.thesis.model.MOEAConfiguration;
import me.soels.thesis.solver.hierarchical.HierarchicalSolver;
import me.soels.thesis.solver.moea.ClusteringProblem;
import me.soels.thesis.solver.moea.MOEASolver;
import me.soels.thesis.solver.moea.VariableDecoder;
import org.moeaframework.Executor;
import org.moeaframework.core.Problem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolverFactory {
    private final VariableDecoder variableDecoder;

    public SolverFactory(VariableDecoder variableDecoder) {
        this.variableDecoder = variableDecoder;
    }

    public Solver createSolver(Evaluation evaluation, EvaluationInput input) {
        var configuration = evaluation.getConfiguration();
        if (configuration instanceof MOEAConfiguration) {
            return createMOEASolver(evaluation, input, (MOEAConfiguration) configuration);
        } else if (configuration instanceof HierarchicalConfiguration) {
            return createHierarhicalSolver(evaluation, input);
        } else {
            throw new IllegalArgumentException("Unknown type of configuration " +
                    configuration.getClass().getSimpleName() + " found for evaluation " + evaluation.getId());
        }
    }

    private HierarchicalSolver createHierarhicalSolver(Evaluation evaluation, EvaluationInput input) {
        // TODO: Implement creation of hierarchical solver
        return new HierarchicalSolver();
    }

    private MOEASolver createMOEASolver(Evaluation evaluation, EvaluationInput input, MOEAConfiguration configuration) {
        var problem = createProblem(evaluation, input, configuration);
        var executor = createExecutor(problem, configuration);
        return new MOEASolver(input, configuration, evaluation.getObjectives(), executor, variableDecoder);
    }

    private Problem createProblem(Evaluation evaluation, EvaluationInput input, MOEAConfiguration configuration) {
        var objectives = evaluation.getObjectives().stream()
                .flatMap(objectiveType -> ObjectiveMapper.getMetricsForObjective(objectiveType).stream())
                .collect(Collectors.toUnmodifiableList());
        return new ClusteringProblem(List.copyOf(objectives), input, configuration, variableDecoder);
    }

    private Executor createExecutor(Problem problem, MOEAConfiguration configuration) {
        return new Executor()
                .distributeOnAllCores()
                .withProblem(problem)
                .withMaxTime(configuration.getMaxTime().orElse(-1L))
                .withMaxEvaluations(configuration.getMaxEvaluations())
                .withAlgorithm(configuration.getAlgorithm().toString());
        // TODO: Hardcode operators: disable crossover, keep mutation, add enhanced mutation of moving multiple connected nodes.
    }
}
