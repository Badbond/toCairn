package me.soels.thesis.clustering;

import me.soels.thesis.clustering.encoding.EncodingType;
import me.soels.thesis.clustering.encoding.VariableDecoder;
import me.soels.thesis.clustering.objectives.Objective;
import me.soels.thesis.model.EvaluationConfiguration;
import me.soels.thesis.model.EvaluationInput;
import org.moeaframework.Executor;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.distributed.DistributedProblem;

import java.util.List;

/**
 * Models the clustering problem.
 * <p>
 * This model is responsible for defining the encoding of genes used by the evolutionary algorithm. This is configurable
 * through the {@link EncodingType} provided.
 * <p>
 * This problem can be parallelized as this problem only maintains state that does not change between evaluations. One
 * can do so using {@link DistributedProblem} or {@link Executor#distributeOnAllCores()}.
 *
 * @see EncodingType
 */
public class ClusteringProblem extends AbstractProblem {
    private final List<Objective> objectives;
    private final EvaluationInput evaluationInput;
    private final EvaluationConfiguration configuration;
    private final VariableDecoder variableDecoder = new VariableDecoder();

    /**
     * Constructs a new instance of the clustering problem
     *
     * @param objectives           the objective functions to evaluate
     * @param analysisInput        the input to cluster
     * @param configuration the configuration for the problem
     */
    public ClusteringProblem(List<Objective> objectives, EvaluationInput analysisInput, EvaluationConfiguration configuration) {
        super(analysisInput.getOtherClasses().size(), objectives.size());
        this.objectives = objectives;
        this.evaluationInput = analysisInput;
        this.configuration = configuration;
    }

    /**
     * Evaluates a solution based on the configured objective functions.
     * <p>
     * Decodes the given solution to a structure understandable for the objective functions, evaluates the objective
     * functions, and sets the objective values on the solution.
     *
     * @param solution the solution to evaluate
     */
    @Override
    public void evaluate(Solution solution) {
        var variables = EncodingUtils.getInt(solution);
        var decodedClustering = variableDecoder.decode(evaluationInput, variables, configuration.getEncodingType());

        if (configuration.getClusterCountLowerBound().isPresent() &&
                decodedClustering.getByCluster().size() < configuration.getClusterCountLowerBound().get()) {
            solution.setConstraint(0, -1); // Too few clusters
            return;
        } else if (configuration.getClusterCountUpperBound().isPresent() &&
                decodedClustering.getByCluster().size() > configuration.getClusterCountUpperBound().get()) {
            solution.setConstraint(0, 1); // Too many clusters
            return;
        }

        for (var i = 0; i < objectives.size(); i++) {
            double objectiveValue = objectives.get(i).calculate(decodedClustering, evaluationInput);
            solution.setObjective(i, objectiveValue);
        }
    }

    /**
     * Constructs the solution structure.
     * <p>
     * This does not do initialization of the initial population as that is depending on the algorithm. This therefore
     * only defines the structure of encoding used. Both cluster-label encoding and locus-adjacency graph encoding
     * have the same typing in terms of variables.
     * <p>
     * Regarding the bounds of the variables, we can not need to place bounds as we normalize our clustering during
     * decoding such that it has increasing cluster numbers. Our bounds are then validates with constraints.
     * They therefore have the same bounds as in cluster-label encoding there can be 1 up to n number of clusters and
     * in locus-adjacency graph encoding every class can be linked to any of the n classes, where n is the amount of
     * classes.
     *
     * @return the solution structure
     * @see EncodingType
     */
    @Override
    public Solution newSolution() {
        // The constraint that we evaluate if the number of desired clusters.
        var solution = new Solution(getNumberOfVariables(), getNumberOfObjectives(), 1);

        for (var i = 0; i < getNumberOfVariables(); i++) {
            // We use floats instead of binary integers as those allow for more mutation/crossover operations,
            // Preliminary investigation showed there is not much of a performance increase into using binary integers
            solution.setVariable(i, EncodingUtils.newInt(0, getUpperbound()));
        }
        return solution;
    }

    private int getUpperbound() {
        if (configuration.getEncodingType() == EncodingType.CLUSTER_LABEL &&
                configuration.getClusterCountUpperBound().isPresent()) {
            return Integer.min(configuration.getClusterCountUpperBound().get(), getNumberOfVariables()) - 1;
        }
        return getNumberOfVariables() - 1;
    }
}