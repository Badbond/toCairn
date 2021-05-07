package me.soels.thesis;

import me.soels.thesis.model.OtherClass;
import org.moeaframework.core.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The clustering as decoded from the {@link Solution} based on {@link EncodingType}.
 */
public final class Clustering {
    private final Map<Integer, List<OtherClass>> clustering = new HashMap<>();

    /**
     * Retrieve the clustering.
     *
     * @return the clustering
     */
    public List<List<OtherClass>> getClustering() {
        return new ArrayList<>(clustering.values());
    }

    /**
     * Add the given class to the specified cluster.
     *
     * @param otherClass    the class to add
     * @param clusterNumber the cluster to add the class to
     */
    public void addToCluster(OtherClass otherClass, int clusterNumber) {
        var cluster = clustering.get(clusterNumber);
        if (cluster == null) {
            cluster = new ArrayList<>();
        }
        cluster.add(otherClass);
        clustering.put(clusterNumber, cluster);
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
