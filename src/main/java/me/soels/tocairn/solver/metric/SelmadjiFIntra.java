package me.soels.tocairn.solver.metric;

import java.util.Set;
import java.util.stream.Collectors;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

/**
 * FIntra metric as proposed by Selmadji et al. (2020).
 * <p>
 * This metric should be maximized but, as the MOEA framework only allows for minimization objectives, we negate the
 * value.
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFIntra extends SelmadjiDataAutonomy {
    @Override
    public double calculate(Clustering clustering) {
        return -1 * clustering.getByCluster().values().stream()
                .mapToDouble(ms -> fintra(ms, clustering))
                .sum();
    }

    private double fintra(Set<OtherClass> microservice, Clustering clustering) {
        var optimizationKey = microservice.stream().map(clazz -> clazz.getId().toString()).sorted().collect(Collectors.joining(""));
        var existingValue = clustering.getOptimizationData().getFIntra().get(optimizationKey);
        if (existingValue != null) {
            return existingValue;
        }

        // Get the cardinality of the set of data classes manipulated by classes in this microservice
        var nbDataManipulatedInMicro = nbDataManipulatedInMicro(microservice);
        if (nbDataManipulatedInMicro == 0) {
            // This microservice does not modify data. Therefore, it will not have any data autonomy
            clustering.getOptimizationData().getFIntra().put(optimizationKey, 0.0);
            return 0.0;
        }

        // Get all the pairs of classes in this microservice excluding self-pairs.
        var pairs = microservice.stream()
                .flatMap(i -> microservice.stream()
                        .filter(j -> !i.equals(j))
                        .map(j -> Pair.of(i, j)))
                .collect(Collectors.toList());

        if (pairs.isEmpty()) {
            // We do not have any pairs in this microservice to calculate this metric with.
            // We set the value to 0.0 to favour microservices with at least two classes.
            clustering.getOptimizationData().getFIntra().put(optimizationKey, 0.0);
            return 0.0;
        }

        // Calculate the metric
        var result = pairs.stream()
                .mapToDouble(pair -> data(pair.getKey(), pair.getValue(), nbDataManipulatedInMicro))
                .sum() / pairs.size();
        clustering.getOptimizationData().getFIntra().put(optimizationKey, result);
        return result;
    }
}
