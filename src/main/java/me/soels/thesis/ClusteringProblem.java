package me.soels.thesis;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import java.util.List;

import static me.soels.thesis.EncodingType.ENCODING_TYPE_ATTRIBUTE_KEY;
import static me.soels.thesis.EncodingType.GRAPH_ADJECENCY;

public class ClusteringProblem extends AbstractProblem {
    private final List<Objective> objectives;
    private final ApplicationInput applicationInput;
    private final EncodingType encodingType;

    /**
     * Constructs a new instance of the clustering problem
     *
     * @param objectives       the objective functions to evaluate
     * @param applicationInput the input to cluster
     * @param encodingType     the type of solution encoding to use
     */
    public ClusteringProblem(List<Objective> objectives, ApplicationInput applicationInput, EncodingType encodingType) {
        super(applicationInput.getClasses().size(), objectives.size());
        this.objectives = objectives;
        this.applicationInput = applicationInput;
        this.encodingType = encodingType;
    }

    @Override
    public void evaluate(Solution solution) {
        // TODO:
        //  Calculation of objectives maybe generic of encoding? Or shall we decode it into a structure
        //  common for the objectives? Probably the latter.
        var decodedClustering = decodeToClusters(solution);

        for (int i = 0; i < objectives.size(); i++) {
            solution.setObjective(i, objectives.get(i).calculate(decodedClustering, applicationInput));
        }
        // TODO: For cluster-label based encoding we need to filter out duplicates somehow by ordering the variable
        //  values in increasing order with fixed class order (maybe the framework does filtering itself?).
    }

    private Clustering decodeToClusters(Solution solution) {
        var variables = EncodingUtils.getInt(solution);
        var result = new Clustering();

        if (encodingType == EncodingType.CLUSTER_LABEL) {
            for (int i = 0; i < variables.length; i++) {
                var variable = variables[i];
                result.addToCluster(variable, applicationInput.getClasses().get(i));
            }
        } else if (encodingType == GRAPH_ADJECENCY) {
            // TODO: Implement graph adjacency decoding
            int a = 0;
        }
        return result;
    }

    /**
     * Constructs the solution structure.
     * <p>
     * This does not do initialization of the initial population as that is depending on the algorithm. This therefore
     * only defines the structure of encoding used. Both cluster-label encoding and locus-adjacency graph encoding
     * have the same typing in terms of variables. They furthermore have the same bounds as well where in cluster-label
     * encoding there can be 1 up to n number of clusters and in locus-adjacency graph encoding every class can be
     * linked to any of the n classes, where n is the amount of classes.
     *
     * @return the solution structure
     */
    @Override
    public Solution newSolution() {
        // TODO: Investigate what FutureSolution does and whether it improves performance
        var solution = new Solution(getNumberOfVariables(), getNumberOfObjectives());
        solution.setAttribute(ENCODING_TYPE_ATTRIBUTE_KEY, encodingType);

        for (int i = 0; i < getNumberOfVariables(); i++) {
            solution.setVariable(i, EncodingUtils.newInt(0, getNumberOfVariables()));
        }
        return solution;
    }
}
