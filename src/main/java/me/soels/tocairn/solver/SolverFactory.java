package me.soels.tocairn.solver;

import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.HierarchicalConfiguration;
import me.soels.tocairn.model.MOEAConfiguration;
import me.soels.tocairn.solver.hierarchical.HierarchicalSolver;
import me.soels.tocairn.solver.moea.ClusteringProblem;
import me.soels.tocairn.solver.moea.MOEASolver;
import me.soels.tocairn.solver.moea.VariableDecoder;
import org.moeaframework.Executor;
import org.moeaframework.core.Problem;
import org.springframework.stereotype.Service;

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
            return createMOEASolver(input, (MOEAConfiguration) configuration);
        } else if (configuration instanceof HierarchicalConfiguration) {
            return createHierarhicalSolver((HierarchicalConfiguration) configuration);
        } else {
            throw new IllegalArgumentException("Unknown type of configuration " +
                    configuration.getClass().getSimpleName() + " found for evaluation " + evaluation.getId());
        }
    }

    private HierarchicalSolver createHierarhicalSolver(HierarchicalConfiguration configuration) {
        return new HierarchicalSolver(configuration);
    }

    private MOEASolver createMOEASolver(EvaluationInput input, MOEAConfiguration configuration) {
        var problem = createProblem(input, configuration);
        var executor = createExecutor(problem, configuration);
        return new MOEASolver(input, configuration, configuration.getMetrics(), executor, variableDecoder);
    }

    private Problem createProblem(EvaluationInput input, MOEAConfiguration configuration) {
        var metrics = configuration.getMetrics().stream()
                .flatMap(metricType -> metricType.getMetrics().stream())
                .collect(Collectors.toUnmodifiableList());
        return new ClusteringProblem(metrics, input, configuration, variableDecoder);
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
