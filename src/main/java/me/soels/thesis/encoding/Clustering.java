package me.soels.thesis.encoding;

import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.OtherClass;
import org.apache.commons.lang3.tuple.Pair;
import org.moeaframework.core.Solution;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The clustering as decoded from the {@link Solution} based on {@link EncodingType}.
 */
public final class Clustering {
    private final Map<Integer, List<OtherClass>> byCluster;
    private final Map<OtherClass, Integer> byClass;

    /**
     * Constructs the clustering.
     * <p>
     * Note that the internal maps and lists of this data structure are made unmodifiable to have an immutable data
     * representation.
     *
     * @param clustering the clustering to create
     */
    Clustering(Map<Integer, List<OtherClass>> clustering) {
        this.byCluster = clustering.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), Collections.unmodifiableList(entry.getValue())))
                .collect(Collectors.toUnmodifiableMap(Pair::getLeft, Pair::getRight));

        var sortedByClass = clustering.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(clazz -> Pair.of(clazz, entry.getKey())))
                .collect(Collectors.toMap(
                        Pair::getKey,
                        Pair::getValue,
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        () -> new TreeMap<>(Comparator.comparing(AbstractClass::getIdentifier))
                ));
        // We use by class also for inspection and visualisation and therefore would like it sorted.
        this.byClass = Collections.unmodifiableSortedMap(sortedByClass);
    }

    /**
     * Retrieve the clustering decoded by cluster.
     * <p>
     * This will return an unmodifiable map of cluster numbers and the associates unmodifiable list of classes contained
     * in that cluster.
     *
     * @return the clustering by cluster
     */
    public Map<Integer, List<OtherClass>> getByCluster() {
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
}
