package me.soels.thesis.objectives;

import me.soels.thesis.encoding.Clustering;
import me.soels.thesis.model.AnalysisModel;

/**
 * Coupling as measured by Carvalho et al. (2020) based off of metrics in the work of Chidamber and Kemerer (1994).
 * <p>
 * They calculate the coupling of a microservice based on summarizing the method-to-method calls. We map this to our
 * own data set which is based on class-to-class dependencies. We summarize the amount of external connections one class
 * has to other classes in a different microservice for every class in a microservice. The total coupling is based on
 * summarization of all microservices as Carvalho et al. define it.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE Transactions on software engineering, 20</i>(6), 476-493.'.
 */
public class CouplingCarvalhoObjective implements OnePurposeMetric {
    @Override
    public double calculate(Clustering clustering, AnalysisModel analysisModel) {
        // TODO: Using mock data we did not consider the direction of the relationship which is important for this metric.
        var coupling = 0.0;
        var clusteringByClass = clustering.getByClass();
        for (var edge : analysisModel.getDependencies()) {
            if (!clusteringByClass.get(edge.getFirst()).equals(clusteringByClass.get(edge.getSecond()))) {
                // TODO: Carvalho uses sc(a,b) here which is the number of calls present in A to B, use static freq.
                coupling++;
            }
        }
        return coupling;
    }
}
