package me.soels.tocairn.solver;

import me.soels.tocairn.model.AHCAConfiguration;
import me.soels.tocairn.model.Evaluation;
import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.MOECAConfiguration;
import me.soels.tocairn.solver.ahca.AHCASolver;
import me.soels.tocairn.solver.moeca.MOECAExecutor;
import me.soels.tocairn.solver.moeca.MOECAProblem;
import me.soels.tocairn.solver.moeca.MOECASolver;
import me.soels.tocairn.solver.moeca.VariableDecoder;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class SolverFactory {
    private final VariableDecoder variableDecoder;

    public SolverFactory(VariableDecoder variableDecoder) {
        this.variableDecoder = variableDecoder;
    }

    public Solver createSolver(Evaluation evaluation, EvaluationInput input) {
        var configuration = evaluation.getConfiguration();
        if (configuration instanceof MOECAConfiguration) {
            return createMOECASolver((MOECAConfiguration) configuration, input);
        } else if (configuration instanceof AHCAConfiguration) {
            return createAHCASolver((AHCAConfiguration) configuration, input);
        } else {
            throw new IllegalArgumentException("Unknown type of configuration " +
                    configuration.getClass().getSimpleName() + " found for evaluation " + evaluation.getId());
        }
    }

    private AHCASolver createAHCASolver(AHCAConfiguration configuration, EvaluationInput input) {
        return new AHCASolver(configuration, input);
    }

    private MOECASolver createMOECASolver(MOECAConfiguration configuration, EvaluationInput input) {
        var problem = createProblem(input, configuration);
        var properties = new Properties();
        properties.put("operator", "pm");
        properties.putAll(configuration.getAdditionalProperties());
        configuration.getPopulationSize().ifPresent(populationSize ->
                properties.put("populationSize", String.valueOf(populationSize)));
        var executor = createExecutor(problem, configuration, properties);
        return new MOECASolver(configuration, configuration.getMetrics(), input, executor, variableDecoder);
    }

    private MOECAProblem createProblem(EvaluationInput input, MOECAConfiguration configuration) {
        return new MOECAProblem(input, configuration, variableDecoder);
    }

    private MOECAExecutor createExecutor(MOECAProblem problem, MOECAConfiguration configuration, Properties properties) {
        var executor = new MOECAExecutor(problem);
        executor.distributeOnAllCores()
                .withProperties(properties)
                .withMaxTime(configuration.getMaxTime().orElse(-1L))
                .withMaxEvaluations(configuration.getMaxEvaluations())
                .withAlgorithm(configuration.getAlgorithm());
        return executor;
    }
}
