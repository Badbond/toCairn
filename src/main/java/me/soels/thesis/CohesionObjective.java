package me.soels.thesis;

import me.soels.thesis.model.AnalysisModel;

public class CohesionObjective implements Objective {
    @Override
    public double calculate(Clustering decodedClustering, AnalysisModel analysisModel) {
        // TODO: Implement measure of cluster cohesion (control-flow) based on combination of Clustering and ApplicationInput
        return -1 * 0.0;
    }
}

// Possibilities:
// 1. From li2019dataflow: RelationalCohesion(RC)(Larman,2012)
// 2. From selmadji2029monolithic: Internal Cohesion
// 3. From carvalho2020performance: Cohesion (based on methods)
// 4. From jin2018requirement (requires api spec): Cohesion at domain level (CHD), cohesion at message level (CHM)
// 5. From bogner2017automatically for service-based systems: Perepletchikov et al. and Shim et al. have Cohesion metrics. See also 3.4 and table 2.
