package me.soels.thesis.services;

import me.soels.thesis.clustering.ClusteringExecutorProvider;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.model.*;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static me.soels.thesis.model.EvaluationStatus.DONE;
import static me.soels.thesis.model.EvaluationStatus.ERRORED;

/**
 * Service responsible for performing a run for a configured evaluation and storing the result.
 */
@Service
public class EvaluationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationRunner.class);
    private final ClusteringExecutorProvider executorProvider;
    private final VariableDecoder decoder;
    private final EvaluationService evaluationService;
    private final AtomicBoolean runnerRunning = new AtomicBoolean(false);

    public EvaluationRunner(ClusteringExecutorProvider executorProvider,
                            VariableDecoder decoder,
                            EvaluationService evaluationService) {
        this.executorProvider = executorProvider;
        this.decoder = decoder;
        this.evaluationService = evaluationService;
    }

    @Async
    public void runEvaluation(Evaluation evaluation) {
        if (!runnerRunning.compareAndSet(false, true)) {
            throw new IllegalArgumentException("An static analysis is already running. The JavaParser library has " +
                    "degraded coverage due to errors when running in parallel. Stopping analysis");
        }

        try {
            LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
            var input = new EvaluationInputBuilder(evaluation.getInputs()).build();
            var executor = executorProvider.getExecutor(evaluation, input);
            runWithExecutor(executor, evaluation)
                    .ifPresent(result -> storeResult(evaluation, input, result));
        } finally {
            runnerRunning.set(false);
        }
    }

    /**
     * Stores the MOEA algorithm result with its solutions, objective values and clusters for the given evaluation.
     *
     * @param evaluation the evaluation to store the result in
     * @param input      the input graph for decoding purposes
     * @param result     the MOEA population result
     */
    private void storeResult(Evaluation evaluation, EvaluationInput input, NondominatedPopulation result) {
        var newResult = new EvaluationResult();
        // TODO: Set metric information (runtime etc.).

        var solutions = StreamSupport.stream(result.spliterator(), false)
                .map(solution -> setupSolution(evaluation, input, solution))
                .collect(Collectors.toList());
        newResult.getSolutions().addAll(solutions);
        evaluation.getResults().add(newResult);
        evaluationService.save(evaluation);
        LOGGER.info("Evaluation run complete. Result id: {}, Solutions: {}, Clusters: {}", newResult.getId(),
                solutions.size(), solutions.stream().mapToInt(sol -> sol.getClusters().size()).sum());
    }

    /**
     * Sets up a single solution and its clusters.
     *
     * @param evaluation the evaluation
     * @param input      the input to use in decoding the variables in the given solution
     * @param solution   the MOEA solution to convert
     * @return the yet unpersisted solution
     */
    private Solution setupSolution(Evaluation evaluation,
                                   EvaluationInput input,
                                   org.moeaframework.core.Solution solution) {
        var newSolution = new Solution();
        var i = 0;
        // Retrieve metric information
        for (var objective : evaluation.getObjectives()) {
            var metrics = executorProvider.getMetricsForObjective(objective);
            var values = Arrays.copyOfRange(solution.getObjectives(), i, i + metrics.size());
            newSolution.getObjectiveValues().put(objective, values);
            i += metrics.size();
        }

        // Set up clusters
        var clustering = decoder.decode(solution, input, evaluation.getConfiguration());
        clustering.getByCluster().entrySet().stream()
                .map(entry -> new Cluster(entry.getKey(), entry.getValue()))
                .forEach(cluster -> newSolution.getClusters().add(cluster));
        return newSolution;
    }

    private Optional<NondominatedPopulation> runWithExecutor(Executor executor, Evaluation evaluation) {
        try {
            // TODO: There is also a runWithSeeds to perform multiple runs.. should I include that maybe?
            var result = executor.run();
            LOGGER.info("The evaluation with ID {} succeeded.", evaluation.getId());
            evaluationService.updateStatus(evaluation, DONE);
            return Optional.of(result);
        } catch (Exception e) {
            LOGGER.error("The evaluation with ID " + evaluation.getId() + " failed.", e);
            evaluationService.updateStatus(evaluation, ERRORED);
            return Optional.empty();
        }
    }
}
