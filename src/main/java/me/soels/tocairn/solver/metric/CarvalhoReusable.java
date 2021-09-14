package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;

import java.util.List;

/**
 * Reusability measurement by Carvalho et al. (2020).
 * <p>
 * This metric should be maximized but, as the MOEA framework only allows for minimization objectives, we negate the
 * value.
 * <p>
 * Instead of checking whether methods in a microservice are reused, we instead take the same approach for whether
 * classes in a microservice have been reused. For this, we look at whether either the API classes have been executed
 * as part of user interaction or whether other microservices have a dynamically used dependency on the microservice.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE Transactions on software engineering, 20</i>(6), 476-493.'.
 **/
public class CarvalhoReusable implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        return -1 * clustering.getByCluster().entrySet().stream()
                .mapToInt(entry -> r(entry.getKey(), entry.getValue(), clustering))
                .sum() / (double) clustering.getByCluster().values().size();
    }

    private int r(Integer clusterNumber, List<OtherClass> microservice, Clustering clustering) {
        var mdu = microservice.stream().anyMatch(OtherClass::isExecutedAPIClass);
        var sc = clustering.getByCluster().entrySet().stream()
                .filter(cluster -> !cluster.getKey().equals(clusterNumber))
                .flatMap(otherMicroservices -> otherMicroservices.getValue().stream())
                .flatMap(outsideClasses -> outsideClasses.getDependenceRelationships().stream())
                .filter(dep -> microservice.contains(dep.getCallee()))
                .anyMatch(dep -> dep.getDynamicFrequency().orElse(0L) > 0);
        return (sc || mdu) ? 1 : 0;
    }
}
