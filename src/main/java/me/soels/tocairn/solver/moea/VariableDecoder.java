package me.soels.tocairn.solver.moea;

import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.MOEAConfiguration;
import me.soels.tocairn.solver.Clustering;
import me.soels.tocairn.solver.ClusteringBuilder;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class VariableDecoder {

    /**
     * Decodes the genes in the solution to the clusters identified.
     *
     * @param solution      the solution to decode
     * @param input         the additional analysis input information required to construct the clustering
     * @param configuration the configuration for the evaluation containing encoding configuration
     * @return the decoded clustering
     */
    public Clustering decode(Solution solution, EvaluationInput input, MOEAConfiguration configuration) {
        var variables = EncodingUtils.getInt(solution);

        switch (configuration.getEncodingType()) {
            case GRAPH_ADJECENCY:
                return decodeGraphAdjacency(input, variables);
            case CLUSTER_LABEL:
                return decodeClusterLabel(input, variables);
            default:
                throw new IllegalStateException("Unknown encoding type " + configuration.getEncodingType());
        }
    }

    /**
     * Decode the given variables based on {@link EncodingType#CLUSTER_LABEL} encoding.
     *
     * @param evaluationInput the additional analysis input information required to construct the clustering
     * @param variables       the variables to decode
     * @return the clustering that the given variables represent
     */
    private Clustering decodeClusterLabel(EvaluationInput evaluationInput, int[] variables) {
        var clusteringBuilder = new ClusteringBuilder();
        var clusterNormalizationMapping = new LinkedHashMap<Integer, Integer>();
        for (var i = 0; i < variables.length; i++) {
            var clusterNumber = variables[i];
            clusterNormalizationMapping.putIfAbsent(clusterNumber, clusterNormalizationMapping.size());
            clusteringBuilder.addToCluster(evaluationInput.getOtherClasses().get(i), clusterNormalizationMapping.get(clusterNumber));
        }
        return clusteringBuilder.build();
    }


    /**
     * Decode the given variables based on {@link EncodingType#GRAPH_ADJECENCY} encoding.
     *
     * @param evaluationInput the additional analysis input information required to construct the clustering
     * @param variables       the variables to decode
     * @return the clustering that the given variables represent
     */
    private Clustering decodeGraphAdjacency(EvaluationInput evaluationInput, int[] variables) {
        var clusteringBuilder = new ClusteringBuilder();
        var nodeClusterPair = new TreeMap<Integer, Integer>();
        var clustersToMerge = new TreeMap<Integer, Integer>(Comparator.reverseOrder());
        var clusterCount = 0;

        for (var i = 0; i < variables.length; i++) {
            var linkedNode = variables[i];
            var existingCurrent = Optional.ofNullable(nodeClusterPair.get(i));
            var existingLinked = Optional.ofNullable(nodeClusterPair.get(linkedNode));

            if (existingCurrent.isPresent() && existingLinked.isEmpty()) {
                // We have not yet seen the linked node, but for the current node a cluster was already created.
                // Add linked node to existing cluster
                nodeClusterPair.put(linkedNode, existingCurrent.get());
                clusteringBuilder.addToCluster(evaluationInput.getOtherClasses().get(linkedNode), existingCurrent.get());
            } else if (existingCurrent.isEmpty() && existingLinked.isPresent()) {
                // We have not yet seen the current node, but for the linked node a cluster was already created.
                // Add current node to existing cluster
                nodeClusterPair.put(i, existingLinked.get());
                clusteringBuilder.addToCluster(evaluationInput.getOtherClasses().get(i), existingLinked.get());
            } else if (existingCurrent.isEmpty()) {
                // We have not seen either node, create a new cluster for both
                nodeClusterPair.put(i, clusterCount);
                clusteringBuilder.addToCluster(evaluationInput.getOtherClasses().get(i), clusterCount);
                nodeClusterPair.put(linkedNode, clusterCount);
                clusteringBuilder.addToCluster(evaluationInput.getOtherClasses().get(linkedNode), clusterCount);
                clusterCount++;
            } else if (!existingCurrent.get().equals(existingLinked.get())) {
                // We already have two different clusters for both the current and linked node. These should be merged.
                clustersToMerge.put(Math.max(existingCurrent.get(), existingLinked.get()),
                        Math.min(existingCurrent.get(), existingLinked.get()));
            } // The else condition results in the two nodes being present and already being part of the same cluster.
        }

        clustersToMerge.forEach(clusteringBuilder::mergeCluster);
        return clusteringBuilder.build();
    }
}
