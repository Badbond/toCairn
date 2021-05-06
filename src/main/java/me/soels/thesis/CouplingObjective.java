package me.soels.thesis;

public class CouplingObjective implements Objective {
    @Override
    public double calculate(Clustering decodedClustering, ApplicationInput applicationInput) {
        // TODO: Implement measure of cluster coupling (control-flow) based on combination of Clustering and ApplicationInput
        return 0.0;
    }
}

// Possibilities:
// 1. From li2019dataflow: Instability(I)(Martin,2002). Combination of AfferentCoupling(Ca)(Martin,2002) & EfferentCoupling(Ce)(Martin,2002)
// Now at mardukhi2013qos of saved works, also check Fritsch2018 and alike.
