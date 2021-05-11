package me.soels.thesis.objectives;

/**
 * Functional interface to identify a metric as a {@link ObjectiveType#ONE_PURPOSE} metric.
 */
public interface OnePurposeMetric extends Objective {
}


// Possibilities for coupling:
// 1. From li2019dataflow: Instability(I)(Martin,2002). Combination of AfferentCoupling(Ca)(Martin,2002) & EfferentCoupling(Ce)(Martin,2002)
// 2. From selmadji2029monolithic: External Coupling (there is also internal coupling, so we might need both)
// 3. From taibi2019monolithic: Coupling Between Object (CBO) metric proposed by Chidamber and Kemerer (Chi-damber and Kemerer, 1994). CBO represents the number of classes coupled with a given class (efferent couplings and afferent couplings).
// 4. From taibi2019monolithic: Coupling between microservice (CBM) also based off of Coupling between modules (CBM) from Lindvall et al.
// 5. From carvalho2020performance: Coupling (based on methods)
// 6. From jin2018functiona (requires api spec): Interface Number (IFN), Operation Number (OPN), Interaction number (IRN)
// 7. From bogner2017automatically for service-based systems: Perepletchikov et al. and Shim et al. have Coupling metrics. See also section 3.3 and table 2.
// 8. From allen1999measuring: module coupling
// 9. From eski2018automatic: modularity: (sum(i e MS) (wei - wai^2)) with wei being fraction of internal edges (cohesion?), wai^2 being fraction of external edges (coupling?)
// See also https://en.wikipedia.org/wiki/Software_package_metrics for the Ca, Ce and I metrics: "Instability (I): The ratio of efferent coupling (Ce) to total coupling (Ce + Ca) such that I = Ce / (Ce + Ca). This metric is an indicator of the package's resilience to change. The range for this metric is 0 to 1, with I=0 indicating a completely stable package and I=1 indicating a completely unstable package."
//
// For now: chosen taibi2019monolithic coupling between microservice. Because:
//      - Li2019 Ca, Ce, I are not based on microservice property
//      - Selmadji2020 has multiple metrics for coupling: perhaps we should still do them all...
//      - Taibi: Note, framework based on dynamic analysis and methods, but metric based on just graph theory.
//      - Carvalho2020: It is based on methods and therefore we need to reason about its applicability to classes (although likely it will be the same metric)
//      - Jin2018: Requires API specification, not suitable for us at this stage.
//      - Bogner2017: STILL NEED TO CHECK
//
//
//
// Possibilities for cohesion:
// 1. From li2019dataflow: RelationalCohesion(RC)(Larman,2012)
// 2. From selmadji2029monolithic: Internal Cohesion
// 3. From carvalho2020performance: Cohesion (based on methods)
// 4. From jin2018requirement (requires api spec): Cohesion at domain level (CHD), cohesion at message level (CHM)
// 5. From bogner2017automatically for service-based systems: Perepletchikov et al. and Shim et al. have Cohesion metrics. See also 3.4 and table 2.

