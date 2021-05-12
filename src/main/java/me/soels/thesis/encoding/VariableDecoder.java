package me.soels.thesis.encoding;

import me.soels.thesis.model.AnalysisModel;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.TreeMap;

public class VariableDecoder {
    /**
     * Decodes the genes in the solution to the clusters identified.
     *
     * @param analysisModel the additional analysis input information required to construct the clustering
     * @param variables     the variables to decode
     * @param encodingType  the type of encoding which the variables represents
     * @return the decoded clustering
     */
    public static Clustering decode(AnalysisModel analysisModel, int[] variables, EncodingType encodingType) {
        switch (encodingType) {
            case GRAPH_ADJECENCY:
                return decodeGraphAdjacency(analysisModel, variables);
            case CLUSTER_LABEL:
                return decodeClusterLabel(analysisModel, variables);
            default:
                throw new IllegalStateException("Unknown encoding type " + encodingType);
        }
    }

    /**
     * Decode the given variables based on {@link EncodingType#CLUSTER_LABEL} encoding.
     *
     * @param analysisModel the additional analysis input information required to construct the clustering
     * @param variables     the variables to decode
     * @return the clustering that the given variables represent
     */
    public static Clustering decodeClusterLabel(AnalysisModel analysisModel, int[] variables) {
        var clusteringBuilder = new ClusteringBuilder();
        var clusterNormalizationMapping = new LinkedHashMap<Integer, Integer>();
        for (int i = 0; i < variables.length; i++) {
            var clusterNumber = variables[i];
            clusterNormalizationMapping.putIfAbsent(clusterNumber, clusterNormalizationMapping.size());
            clusteringBuilder.addToCluster(analysisModel.getOtherClasses().get(i), clusterNormalizationMapping.get(clusterNumber));
        }
        return clusteringBuilder.build();
    }


    /**
     * Decode the given variables based on {@link EncodingType#GRAPH_ADJECENCY} encoding.
     *
     * @param analysisModel the additional analysis input information required to construct the clustering
     * @param variables     the variables to decode
     * @return the clustering that the given variables represent
     */
    public static Clustering decodeGraphAdjacency(AnalysisModel analysisModel, int[] variables) {
        var clusteringBuilder = new ClusteringBuilder();
        var nodeClusterPair = new TreeMap<Integer, Integer>();
        var clustersToMerge = new TreeMap<Integer, Integer>(Comparator.reverseOrder());
        int clusterCount = 0;

        for (int i = 0; i < variables.length; i++) {
            var linkedNode = variables[i];
            var existingCurrent = Optional.ofNullable(nodeClusterPair.get(i));
            var existingLinked = Optional.ofNullable(nodeClusterPair.get(linkedNode));

            if (existingCurrent.isPresent() && existingLinked.isEmpty()) {
                // We have not yet seen the linked node, but for the current node a cluster was already created.
                // Add linked node to existing cluster
                nodeClusterPair.put(linkedNode, existingCurrent.get());
                clusteringBuilder.addToCluster(analysisModel.getOtherClasses().get(linkedNode), existingCurrent.get());
            } else if (existingCurrent.isEmpty() && existingLinked.isPresent()) {
                // We have not yet seen the current node, but for the linked node a cluster was already created.
                // Add current node to existing cluster
                nodeClusterPair.put(i, existingLinked.get());
                clusteringBuilder.addToCluster(analysisModel.getOtherClasses().get(i), existingLinked.get());
            } else if (existingCurrent.isEmpty()) {
                // We have not seen either node, create a new cluster for both
                nodeClusterPair.put(i, clusterCount);
                clusteringBuilder.addToCluster(analysisModel.getOtherClasses().get(i), clusterCount);
                nodeClusterPair.put(linkedNode, clusterCount);
                clusteringBuilder.addToCluster(analysisModel.getOtherClasses().get(linkedNode), clusterCount);
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
