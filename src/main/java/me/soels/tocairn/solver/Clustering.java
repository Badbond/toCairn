package me.soels.tocairn.solver;

import com.google.common.collect.ImmutableMap;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.moeca.EncodingType;
import org.apache.commons.lang3.tuple.Pair;
import org.moeaframework.core.Solution;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * The clustering as decoded from the {@link Solution} based on {@link EncodingType}.
 */
public final class Clustering {
    // The clustering itself modelled in two ways
    private final Map<Integer, Set<OtherClass>> byCluster;
    private final Map<OtherClass, Integer> byClass;
    private final OptimizationData optimizationData;

    /**
     * Constructs the clustering.
     * <p>
     * Note that the internal maps and lists of this data structure are made unmodifiable to have an immutable data
     * representation.
     *
     * @param clustering       the clustering to create
     * @param optimizationData optimization data across all clusterings
     */
    Clustering(Map<Integer, Set<OtherClass>> clustering, OptimizationData optimizationData) {
        this.optimizationData = optimizationData;
        this.byCluster = ImmutableMap.copyOf(clustering);
        this.byClass = clustering.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(clazz -> Pair.of(clazz, entry.getKey())))
                .collect(toImmutableMap(Pair::getKey, Pair::getValue));
    }

    /**
     * Retrieve the clustering decoded by cluster.
     * <p>
     * This will return an unmodifiable map of cluster numbers and the associates unmodifiable list of classes contained
     * in that cluster.
     *
     * @return the clustering by cluster
     */
    public Map<Integer, Set<OtherClass>> getByCluster() {
        return byCluster;
    }

    /**
     * Retrieve the clustering decoded by class.
     * <p>
     * This will return an unmodifiable map of the classes analyzed and the cluster they were assigned to.
     *
     * @return the clustering by class
     */
    public Map<OtherClass, Integer> getByClass() {
        return byClass;
    }

    /**
     * Returns the optimization data for metric calculation.
     *
     * @return optimization data
     */
    public OptimizationData getOptimizationData() {
        return optimizationData;
    }
}
