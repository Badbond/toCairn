package me.soels.tocairn.solver;


import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Models optimization data during metric calculation.
 * <p>
 * This class is thread-safe for the purpose to be shared among threads performing the calculations and populating
 * this data. The only except to this is the {@link #getNbTotalCalls()} as this is always calculated with the initial
 * clustering as input for the algorithms which is not yet multi-threaded.
 */
@Getter
public class OptimizationData {
    // Calculations performed based on microservices that can be reused in other clusterings, optimization.
    // Key is a lexicographic order of class UUIDs combined for the microservice.
    // Value is the metric for that microservice
    private final Map<String, Double> fInter = new ConcurrentHashMap<>();
    private final Map<String, Double> fIntra = new ConcurrentHashMap<>();
    private final Map<String, Double> fOne = new ConcurrentHashMap<>();
    private final Map<String, Double> fAutonomy = new ConcurrentHashMap<>();

    // Data that is calculated once for the whole, regardless of how it is clustered.
    private Long nbTotalCalls;

    public void setNbTotalCalls(long totalNbCalls) {
        this.nbTotalCalls = totalNbCalls;
    }

    public OptimizationData copy() {
        var copy = new OptimizationData();
        copy.setNbTotalCalls(this.nbTotalCalls);
        copy.getFInter().putAll(this.fInter);
        copy.getFIntra().putAll(this.fIntra);
        copy.getFOne().putAll(this.fOne);
        copy.getFAutonomy().putAll(this.fAutonomy);
        return copy;
    }

    public void clearMicroservice(String key) {
        fInter.remove(key);
        fIntra.remove(key);
        fOne.remove(key);
        fAutonomy.remove(key);
    }
}
