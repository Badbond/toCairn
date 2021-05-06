package me.soels.thesis;

import org.moeaframework.core.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The clustering as decoded from the {@link Solution} based on {@link EncodingType}.
 * <p>
 * This clustering exposes a list of clusters containing the nodes as given in {@link ApplicationInput#getClasses()}.
 */
public final class Clustering {
    private final Map<Integer, List<String>> clustering = new HashMap<>();

    /**
     * Retrieve the clustering.
     *
     * @return the clustering
     */
    public List<List<String>> getClustering() {
        return new ArrayList<>(clustering.values());
    }

    /**
     * Add the given node to the specified cluster.
     *
     * @param node          the node to add
     * @param clusterNumber the cluster to add the node to
     */
    public void addToCluster(String node, int clusterNumber) {
        var cluster = clustering.get(clusterNumber);
        if (cluster == null) {
            cluster = new ArrayList<>();
        }
        cluster.add(node);
        clustering.put(clusterNumber, cluster);
    }

    /**
     * Merge the given {@code source} cluster into the given {@code target} cluster.
     *
     * @param source the nodes to transfer
     * @param target the cluster to add the nodes to
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

    /**
     * Normalizes the clusters to be of increasing order by key.
     */
    public void normalize() {
        for (int i = 0; i < clustering.size(); i++) {
            if (!clustering.containsKey(i)) {
                final var currentKey = i;
                var nextKey = clustering.keySet().stream()
                        .filter(key -> key > currentKey)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Could not find cluster greater than " +
                                currentKey + ", but there are at least " + clustering.size() + " clusters."));
                clustering.put(i, clustering.get(nextKey));
                clustering.remove(nextKey);
            }
        }
    }
}
