package me.soels.thesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Clustering {
    private final Map<Integer, List<String>> clustering = new HashMap<>();

    /**
     * Retrieve the clustering.
     * <p>
     * TODO:
     * Make comment that this automatically results in index normalization but that for proper normalization,
     * the nodes have to be added in the correct order as well. Or even better, make this less error-prone.
     *
     * @return the normalized clustering
     */
    public List<List<String>> getClustering() {
        return new ArrayList<>(clustering.values());
    }

    /**
     * @param index
     * @param node
     */
    public void addToCluster(int index, String node) {
        var cluster = clustering.get(index);
        if (cluster == null) {
            cluster = new ArrayList<>();
        }
        cluster.add(node);
        clustering.put(index, cluster);
    }

    /**
     * @param source
     * @param target
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
     * Normalizes the clusters to be of increasing order by key
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
