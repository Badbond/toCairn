package me.soels.thesis.clustering.encoding;

import me.soels.thesis.model.OtherClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder for {@link Clustering} to allow for injecting into, merging and normalizing the stored clustering.
 */
public final class ClusteringBuilder {
    private final Map<Integer, List<OtherClass>> clustering = new HashMap<>();

    /**
     * Builds the normalized clustering.
     *
     * @return the normalized clustering
     */
    public Clustering build() {
        return new Clustering(normalize(clustering));
    }

    /**
     * Normalizes the clustering by cluster label to have cluster label from 0 to {@code n} with {@code n} being
     * the amount of clusters.
     *
     * @param clustering the clustering to normalize
     * @return the normalized clustering
     */
    public Map<Integer, List<OtherClass>> normalize(Map<Integer, List<OtherClass>> clustering) {
        var result = new HashMap<Integer, List<OtherClass>>(clustering.size());
        for (var i = 0; i < clustering.size(); i++) {
            result.put(i, new ArrayList<>(clustering.values()).get(i));
        }
        return result;
    }

    /**
     * Add the given class to the specified cluster.
     *
     * @param otherClass    the class to add
     * @param clusterNumber the cluster to add the class to
     */
    public void addToCluster(OtherClass otherClass, int clusterNumber) {
        var cluster = clustering.computeIfAbsent(clusterNumber, key -> new ArrayList<>());
        if (!cluster.contains(otherClass)) {
            cluster.add(otherClass);
        }
    }

    /**
     * Merge the given {@code source} cluster into the given {@code target} cluster.
     *
     * @param source the cluster number to extract the classes from
     * @param target the cluster number to the extracted classes to
     */
    public void mergeCluster(int source, int target) {
        if (!clustering.containsKey(source)) {
            throw new IllegalStateException("Could not find clustering with id " + source);
        } else if (!clustering.containsKey(target)) {
            throw new IllegalStateException("Could not find clustering with id " + target);
        }

        clustering.get(target).addAll(clustering.get(source));
        clustering.remove(source);
    }
}
