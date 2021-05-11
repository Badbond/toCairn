package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coupling between microservices as suggested by Taibi, D. & Systä, K. (2019). Based off of Coupling between modules
 * from Lindvall et al. (2003).
 * <p>
 * The metric is calculated per microservice identified and is averaged over the whole solution as the authors
 * compared the different solutions that way as well. TODO: Is average fine or should we use euclidean distance?
 * <p>
 * TODO: It is very hard to get 0.0 out of this metric as the algorithm does not attempt to cluster with only 1 cluster..
 * <p>
 * See related work 'Taibi, D., & Systä, K. (2019, May). From Monolithic Systems to Microservices: A Decomposition
 * Framework based on Process Mining. In <i>CLOSER</i> (pp. 153-164).'
 */
public class CouplingBetweenMicroservicesObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, AnalysisModel analysisModel) {
        Map<Integer, List<Integer>> interClusterDependencies = clustering.getByCluster().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>()));
        var clusteringByClass = clustering.getByClass();
        for (var edge : analysisModel.getDependencies()) {
            var classA = edge.getFirst();
            var classB = edge.getSecond();

            if (clusteringByClass.get(classA).equals(clusteringByClass.get(classB))) {
                // Same cluster
                continue;
            }

            addDependency(interClusterDependencies, clusteringByClass.get(classA), clusteringByClass.get(classB));
            addDependency(interClusterDependencies, clusteringByClass.get(classB), clusteringByClass.get(classA));
        }

        return clustering.getByCluster().entrySet().stream()
                .mapToDouble(entry -> interClusterDependencies.get(entry.getKey()).size() / (double) entry.getValue().size())
                .average()
                .orElseThrow(() -> new IllegalStateException("Could not create average CBM for this solution"));
    }

    private void addDependency(Map<Integer, List<Integer>> interClusterDependencies,
                               Integer clusterA,
                               Integer clusterB) {
        // Add dependency between cluster of class A and cluster of class B if they did not exist before
        // as Taibi & Systä only count once.
        // TODO:
        //  If 'external link' is interpreted as microservice, then we should only add once link is not there yet.
        //  If is is interpreted as external API, perhaps calculating class-to-class dependencies is better.
        //  The former is really odd as simple-graph with 2 clusters will always be 0.2 as it will be counted as 1 link between 5 classes.
//        if (!interClusterDependencies.get(clusterB).contains(clusterA)) {
        interClusterDependencies.get(clusterB).add(clusterA);
//        }
    }
}

// Possibilities:
// 1. From li2019dataflow: Instability(I)(Martin,2002). Combination of AfferentCoupling(Ca)(Martin,2002) & EfferentCoupling(Ce)(Martin,2002)
// 2. From selmadji2029monolithic: External Coupling (there is also internal coupling, so we might need both)
// 3. From taibi2019monolithic: Coupling Between Object (CBO) metric proposed by Chidamber and Kemerer (Chi-damber and Kemerer, 1994). CBO represents the number of classes coupled with a given class (efferent couplings and afferent couplings).
// 4. From taibi2019monolithic: Coupling between microservice (CBM) also based off of Coupling between modules (CBM) from Lindvall et al.
// 5. From carvalho2020performance: Coupling (based on methods)
// 6. From jin2018functiona (requires api spec): Interface Number (IFN), Operation Number (OPN), Interaction number (IRN)
// 7. From bogner2017automatically for service-based systems: Perepletchikov et al. and Shim et al. have Coupling metrics. See also section 3.3 and table 2.
// 8. From allen1999measuring: module coupling
// See also https://en.wikipedia.org/wiki/Software_package_metrics for the Ca, Ce and I metrics: "Instability (I): The ratio of efferent coupling (Ce) to total coupling (Ce + Ca) such that I = Ce / (Ce + Ca). This metric is an indicator of the package's resilience to change. The range for this metric is 0 to 1, with I=0 indicating a completely stable package and I=1 indicating a completely unstable package."

// For now: chosen taibi2019monolithic coupling between microservice. Because:
//      - Li2019 Ca, Ce, I are not based on microservice property
//      - Selmadji2020 has multiple metrics for coupling: perhaps we should still do them all...
//      - Taibi: Note, framework based on dynamic analysis and methods, but metric based on just graph theory.
//      - Carvalho2020: It is based on methods and therefore we need to reason about its applicability to classes (although likely it will be the same metric)
//      - Jin2018: Requires API specification, not suitable for us at this stage.
//      - Bogner2017: STILL NEED TO CHECK
