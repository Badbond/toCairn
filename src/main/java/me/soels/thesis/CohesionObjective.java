package me.soels.thesis;

public class CohesionObjective implements Objective {
    @Override
    public double calculate(Clustering decodedClustering, ApplicationInput applicationInput) {
        // TODO: Implement measure of cluster cohesion (control-flow) based on combination of Clustering and ApplicationInput
        return -1 * 0.0;
    }
}

// Possibilities:
// 1. From li2019dataflow: RelationalCohesion(RC)(Larman,2012)
// Now at mardukhi2013qos of saved works, also check Fritsch2018 and alike.

