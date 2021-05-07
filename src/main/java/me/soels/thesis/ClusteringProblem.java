package me.soels.thesis;

import me.soels.thesis.model.AnalysisModel;
import org.moeaframework.Executor;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.distributed.DistributedProblem;

import java.util.*;

/**
 * Models the clustering problem.
 * <p>
 * This model is responsible for defining the encoding of genes used by the evolutionary algorithm. This is configurable
 * through the {@link EncodingType} provided.
 * <p>
 * This problem can be parallelized using {@link DistributedProblem} or {@link Executor#distributeOnAllCores()}.
 *
 * @see EncodingType
 */
public class ClusteringProblem extends AbstractProblem {
    private final List<Objective> objectives;
    private final AnalysisModel analysisModel;
    private final EncodingType encodingType;

    /**
     * Constructs a new instance of the clustering problem
     *
     * @param objectives    the objective functions to evaluate
     * @param analysisModel the input to cluster
     * @param encodingType  the type of solution encoding to use
     */
    public ClusteringProblem(List<Objective> objectives, AnalysisModel analysisModel, EncodingType encodingType) {
        super(analysisModel.getAllClasses().size(), objectives.size());
        this.objectives = objectives;
        this.analysisModel = analysisModel;
        this.encodingType = encodingType;
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
        var decodedClustering = decodeToClusters(solution);

        for (int i = 0; i < objectives.size(); i++) {
            solution.setObjective(i, objectives.get(i).calculate(decodedClustering, analysisModel));
        }
    }

    /**
     * Decodes the genes in the solution to the clusters identified.
     * <p>
     * The resulting clusters are also normalized in order of the {@link #analysisModel} nodes with increasing
     * incremental numbers starting from 0. This is to also make deduplication of redundant solutions possible. For
     * example, both A3-B2-C3 and A9-B3-C9 will map to A0-B1-C0 (in case of cluster-label encoding).
     * TODO: Depending on the deduplication support, we might not need to normalize if it does not benefit us.
     *
     * @param solution the clustering generated by the evolutionary algorithm
     * @return the decoded and normalized clustering
     */
    private Clustering decodeToClusters(Solution solution) {
        var variables = EncodingUtils.getInt(solution);

        switch (encodingType) {
            case CLUSTER_LABEL:
                return decodeClusterLabelEncoding(variables);
            case GRAPH_ADJECENCY:
                return decodeGraphAdjacencyEncoding(variables);
            default:
                throw new IllegalStateException("Unsupported encoding type: " + encodingType);
        }
    }

    private Clustering decodeClusterLabelEncoding(int[] variables) {
        var clusterNormalizationMapping = new LinkedHashMap<Integer, Integer>();
        var result = new Clustering();
        for (int i = 0; i < variables.length; i++) {
            var clusterNumber = variables[i];
            clusterNormalizationMapping.putIfAbsent(clusterNumber, clusterNormalizationMapping.size());
            result.addToCluster(analysisModel.getOtherClasses().get(i), clusterNormalizationMapping.get(clusterNumber));
        }
        return result;
    }

    private Clustering decodeGraphAdjacencyEncoding(int[] variables) {
        var nodeClusterPair = new LinkedHashMap<Integer, Integer>(variables.length);
        var clustersToMerge = new TreeMap<Integer, Integer>(Comparator.reverseOrder());
        var result = new Clustering();
        int clusterCount = 0;

        for (int i = 0; i < variables.length; i++) {
            var linkedNode = variables[i];
            var existingCurrent = Optional.ofNullable(nodeClusterPair.get(i));
            var existingLinked = Optional.ofNullable(nodeClusterPair.get(linkedNode));

            if (existingCurrent.isPresent() && existingLinked.isEmpty()) {
                // We have not yet seen the linked node, but for the current node a cluster was already created.
                // Add linked node to existing cluster
                nodeClusterPair.put(linkedNode, existingCurrent.get());
            } else if (existingCurrent.isEmpty() && existingLinked.isPresent()) {
                // We have not yet seen the current node, but for the linked node a cluster was already created.
                // Add current node to existing cluster
                nodeClusterPair.put(i, existingLinked.get());
            } else if (existingCurrent.isEmpty()) {
                // We have not seen either node, create a new cluster for both
                nodeClusterPair.put(i, clusterCount);
                nodeClusterPair.put(linkedNode, clusterCount);
                clusterCount++;
            } else if (!existingCurrent.get().equals(existingLinked.get())) {
                // We already have two different clusters for both the current and linked node. These should be merged.
                clustersToMerge.put(Math.max(existingCurrent.get(), existingLinked.get()),
                        Math.min(existingCurrent.get(), existingLinked.get()));
            } // The else condition results in the two nodes being present and already being part of the same cluster.
        }

        // TODO: Perhaps we can do this more efficiently within the previous loop.
        nodeClusterPair.forEach((index, cluster) -> result.addToCluster(analysisModel.getOtherClasses().get(index), cluster));
        clustersToMerge.forEach(result::mergeCluster);
        result.normalize();
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
     * @see EncodingType
     */
    @Override
    public Solution newSolution() {
        var solution = new Solution(getNumberOfVariables(), getNumberOfObjectives());

        for (int i = 0; i < getNumberOfVariables(); i++) {
            // TODO: Investigate if binary int is more efficient; do watch out for exclusive/inclusive bounds
            solution.setVariable(i, EncodingUtils.newInt(0, getNumberOfVariables() - 1));
        }
        return solution;
    }
}
