package me.soels.tocairn.solver;

import me.soels.tocairn.model.OtherClass;

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
     * Creates a new {@link Clustering} builder without any data.
     */
    public ClusteringBuilder() {
    }

    /**
     * Creates a new {@link Clustering} builder based on a previous clustering.
     * <p>
     * This will copy the clustering data s.t. it will not be altered.
     *
     * @param clustering the clustering
     */
    public ClusteringBuilder(Clustering clustering) {
        clustering.getByCluster().forEach((key, value) -> this.clustering.put(key, new ArrayList<>(value)));
    }

    /**
     * Builds the normalized clustering.
     *
     * @return the normalized clustering
     */
    public Clustering build() {
        return new Clustering(normalize(clustering));
    }

    /**
     * Normalizes the clustering by cluster label to have cluster label from 0 to {@code n - 1} with {@code n} being
     * the amount of clusters.
     *
     * @param clustering the clustering to normalize
     * @return the normalized clustering
     */
    public Map<Integer, List<OtherClass>> normalize(Map<Integer, List<OtherClass>> clustering) {
        Map<Integer, List<OtherClass>> result = new HashMap<>();
        var counter = 0;
        for (var value : clustering.values()) {
            result.put(counter++, value);
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
