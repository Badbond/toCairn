package me.soels.thesis;

import me.soels.thesis.model.AnalysisModel;

public class CouplingObjective implements Objective {
    @Override
    public double calculate(Clustering decodedClustering, AnalysisModel analysisModel) {
        // TODO: Implement measure of cluster coupling (control-flow) based on combination of Clustering and ApplicationInput
        return 0.0;
    }
}

// Possibilities:
// 1. From li2019dataflow: Instability(I)(Martin,2002). Combination of AfferentCoupling(Ca)(Martin,2002) & EfferentCoupling(Ce)(Martin,2002)
// 2. From selmadji2029monolithic: External Coupling (there is also internal coupling, so we might need both)
// 3. From taibi2019monolithic: well-known Coupling Between Object (CBO)metric  proposed  by  Chidamber  and  Kemerer  (Chi-damber  and  Kemerer,  1994).    CBO  represents  the number of classes coupled with a given class (efferent couplings and afferent couplings).
// 4. From taibi2019monolithic: Coupling between microservice (CBM)