package me.soels.tocairn.solver.metric;

import java.util.Set;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.DependenceRelationship;
import me.soels.tocairn.solver.Clustering;

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
public class CarvalhoCoupling implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        return clustering.getByCluster().values().stream()
                .mapToDouble(this::calculateCoupling)
                .sum();
    }

    private double calculateCoupling(Set<? extends AbstractClass> microservice) {
        return microservice.stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream()
                        // Only include relationships to other clusters
                        .filter(relationship -> !microservice.contains(relationship.getCallee())))
                // This metric uses the frequency of calls to other units
                .mapToDouble(DependenceRelationship::getStaticFrequency)
                .sum();
    }
}
