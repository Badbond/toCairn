package me.soels.thesis.solver.moea;

import me.soels.thesis.model.*;
import me.soels.thesis.solver.Solver;
import me.soels.thesis.solver.metric.MetricType;
import org.moeaframework.Analyzer;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MOEASolver implements Solver {
    private final EvaluationInput input;
    private final MOEAConfiguration configuration;
    private final Set<MetricType> metricTypes;
    private final Executor executor;
    private final VariableDecoder decoder;

    public MOEASolver(EvaluationInput input,
                      MOEAConfiguration configuration,
                      Set<MetricType> metricTypes, Executor executor,
                      VariableDecoder decoder) {
        this.input = input;
        this.configuration = configuration;
        this.metricTypes = metricTypes;
        this.executor = executor;
        this.decoder = decoder;
    }

    @Override
    public EvaluationResult run(EvaluationInput input) {
        var population = executor.run();
        var analysis = performAnalysis(population, executor, configuration.getAlgorithm());
        return createResult(population, analysis);
    }

    private EvaluationResult createResult(NondominatedPopulation population, Analyzer.AlgorithmResult analysisResults) {
        var result = new MOEAEvaluationResult();

        analysisResults.getIndicators().stream()
                .map(analysisResults::get)
                .forEach(indicator -> result.getPopulationMetrics().put(indicator.getIndicator(), indicator.getValues()[0]));

        var solutions = StreamSupport.stream(population.spliterator(), false)
                .map(solution -> setupSolution(input, solution))
                .collect(Collectors.toList());
        result.getSolutions().addAll(solutions);
        return result;
    }

    private Analyzer.AlgorithmResult performAnalysis(NondominatedPopulation result, Executor executor, EvolutionaryAlgorithm algorithm) {
        return new Analyzer()
                .withSameProblemAs(executor)
                .add(algorithm.toString(), result)
                .includeAllMetrics()
                .showAll()
                .getAnalysis()
                .get(algorithm.toString());
    }

    /**
     * Sets up a single solution and its clusters from the MOEA solution output.
     *
     * @param input    the input to use in decoding the variables in the given solution
     * @param solution the MOEA solution to convert
     * @return the yet unpersisted solution
     */
    private Solution setupSolution(EvaluationInput input,
                                   org.moeaframework.core.Solution solution) {
        var newSolution = new Solution();
        var i = 0;
        // Retrieve metric information
        for (var metricType : metricTypes) {
            var metrics = metricType.getMetrics();
            var values = Arrays.copyOfRange(solution.getObjectives(), i, i + metrics.size());
            newSolution.getObjectiveValues().put(metricType, values);
            i += metrics.size();
        }

        // Set up clusters
        var clustering = decoder.decode(solution, input, configuration);
        clustering.getByCluster().entrySet().stream()
                .map(entry -> new Microservice(entry.getKey(), entry.getValue()))
                .forEach(microservice -> newSolution.getMicroservices().add(microservice));
        return newSolution;
    }
}
