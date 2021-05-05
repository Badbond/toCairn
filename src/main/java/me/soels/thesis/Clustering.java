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
     *  Make comment that this automatically results in index normalization but that for proper normalization,
     *  the nodes have to be added in the correct order as well. Or even better, make this less error-prone.
     *
     * @return the normalized clustering
     */
    public List<List<String>> getClustering() {
        return new ArrayList<>(clustering.values());
    }

    public void addToCluster(int index, String node) {
        var cluster = clustering.get(index);
        if (cluster == null) {
            cluster = new ArrayList<>();
        }
        cluster.add(node);
        clustering.put(index, cluster);
    }
}
