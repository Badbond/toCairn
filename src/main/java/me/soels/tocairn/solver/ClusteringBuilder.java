package me.soels.tocairn.solver;

import me.soels.tocairn.model.DependenceRelationship;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.model.Solution;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A builder for {@link Clustering} to allow for injecting into, merging and normalizing the stored clustering.
 */
public final class ClusteringBuilder {
    private final Map<Integer, Set<OtherClass>> clustering = new HashMap<>();
    private OptimizationData optimizationData;

    /**
     * Creates a new {@link Clustering} builder without any data.
     */
    public ClusteringBuilder() {
        this.optimizationData = new OptimizationData();
    }

    /**
     * Creates a new {@link Clustering} builder based on an existing solution.
     *
     * @param solution the solution to create a clustering builder from
     */
    public ClusteringBuilder(Solution solution) {
        solution.getMicroservices().forEach(ms -> clustering.put(ms.getMicroserviceNumber(), ms.getClasses()));
        this.optimizationData = new OptimizationData();
    }

    /**
     * Creates a new {@link Clustering} builder based on a previous clustering.
     * <p>
     * This will copy the clustering data s.t. it will not be altered.
     *
     * @param clustering the clustering
     */
    public ClusteringBuilder(Clustering clustering) {
        clustering.getByCluster().forEach((key, value) -> this.clustering.put(key, new HashSet<>(value)));
        this.optimizationData = clustering.getOptimizationData().copy();
    }

    /**
     * Builds the normalized clustering.
     *
     * @return the normalized clustering
     */
    public Clustering build() {
        if (optimizationData.getNbTotalCalls() == null) {
            optimizationData.setNbTotalCalls(getTotalNbCalls());
        }
        return new Clustering(normalize(clustering), optimizationData);
    }

    /**
     * Normalizes the clustering by cluster label to have cluster label from 0 to {@code n - 1} with {@code n} being
     * the amount of clusters.
     *
     * @param clustering the clustering to normalize
     * @return the normalized clustering
     */
    public Map<Integer, Set<OtherClass>> normalize(Map<Integer, Set<OtherClass>> clustering) {
        Map<Integer, Set<OtherClass>> result = new HashMap<>();
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
        var cluster = clustering.computeIfAbsent(clusterNumber, key -> new HashSet<>());
        cluster.add(otherClass);
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

        var targetCluster = clustering.get(target);
        var sourceCluster = clustering.get(source);

        // To optimize optimization data, clear the merged clusters
        optimizationData.clearMicroservice(targetCluster.stream()
                .map(clazz -> clazz.getId().toString()).sorted()
                .collect(Collectors.joining("")));
        optimizationData.clearMicroservice(sourceCluster.stream()
                .map(clazz -> clazz.getId().toString()).sorted()
                .collect(Collectors.joining("")));

        targetCluster.addAll(sourceCluster);
        clustering.remove(source);
    }

    /**
     * Returns the total number of calls made in the application between {@link OtherClass}.
     *
     * @return the total amount of calls made in the application
     */
    private long getTotalNbCalls() {
        return clustering.values().stream()
                .flatMap(Collection::stream)
                .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                .mapToLong(DependenceRelationship::getStaticFrequency)
                .sum();
    }

    /**
     * Sets optimization data for the resulting clustering
     *
     * @param optimizationData the optimization data to set
     * @return this clustering builder
     */
    public ClusteringBuilder withOptimizationData(@Nullable OptimizationData optimizationData) {
        if (optimizationData != null) {
            this.optimizationData = optimizationData;
        }
        return this;
    }
}
