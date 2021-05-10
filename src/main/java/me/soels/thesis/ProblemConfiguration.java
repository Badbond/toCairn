package me.soels.thesis;

import me.soels.thesis.encoding.EncodingType;

import java.util.Optional;

public final class ProblemConfiguration {
    private final EncodingType encodingType;
    private final Integer clusterCountLowerBound;
    private final Integer clusterCountUpperBound;

    /**
     * Creates a configuration for the clustering problem including encoding type and cluster count bounds.
     * <p>
     * Set the lower bound and upper bound to the same value to fix the amount of clusters to create.
     * <p>
     * The amount of clusters created will never exceed the amount of classes to cluster even though the upper bound was
     * higher.
     *
     * @param encodingType           the type of encoding to use
     * @param clusterCountLowerBound the minimum amount of clusters to create, null for no lower bound
     * @param clusterCountUpperBound the maximum amount of clusters to create, null for no upper bound
     */
    public ProblemConfiguration(EncodingType encodingType, Integer clusterCountLowerBound, Integer clusterCountUpperBound) {
        if (clusterCountLowerBound != null && clusterCountLowerBound < 1) {
            throw new IllegalArgumentException("We need to construct at least 1 cluster");
        } else if (clusterCountLowerBound != null && clusterCountUpperBound != null &&
                clusterCountUpperBound < clusterCountLowerBound) {
            throw new IllegalArgumentException("Cluster count upper bound needs to be greater than its lower bound");
        }

        this.clusterCountLowerBound = clusterCountLowerBound;
        this.clusterCountUpperBound = clusterCountUpperBound;
        this.encodingType = encodingType;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public Optional<Integer> getClusterCountLowerBound() {
        return Optional.ofNullable(clusterCountLowerBound);
    }

    public Optional<Integer> getClusterCountUpperBound() {
        return Optional.ofNullable(clusterCountUpperBound);
    }
}