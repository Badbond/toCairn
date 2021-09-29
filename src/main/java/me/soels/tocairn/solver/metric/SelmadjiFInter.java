package me.soels.tocairn.solver.metric;

import java.util.Set;
import java.util.stream.Collectors;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

/**
 * FInter metric as proposed by Selmadji et al. (2020).
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFInter extends SelmadjiDataAutonomy {
    @Override
    public double calculate(Clustering clustering) {
        return clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> finter(microservice, clustering))
                .sum();
    }

    private double finter(Set<OtherClass> microservice, Clustering clustering) {
        var optimizationKey = microservice.stream().map(clazz -> clazz.getId().toString()).sorted().collect(Collectors.joining(""));
        var existingValue = clustering.getOptimizationData().getFInter().get(optimizationKey);
        if (existingValue != null) {
            return existingValue;
        }

        // Get the cardinality of the set of data classes manipulated by classes in this microservice
        var nbDataManipulatedInMicro = nbDataManipulatedInMicro(microservice);
        if (nbDataManipulatedInMicro == 0) {
            // This microservice does not modify data. Therefore, it will not have any data autonomy
            clustering.getOptimizationData().getFInter().put(optimizationKey, 0.0);
            return 0.0;
        }

        // Get all the classes that do not belong to this microservice.
        var externalClasses = clustering.getByClass().keySet().stream()
                .filter(clazz -> !microservice.contains(clazz))
                .collect(Collectors.toList());
        if (externalClasses.isEmpty()) {
            // There are no more external classes, we have all classes in one microservice, i.e. monolith.
            // Set FInter to 0.
            clustering.getOptimizationData().getFInter().put(optimizationKey, 0.0);
            return 0.0;
        }

        // Get all the pairs of classes in this microservice with those of external classes.
        var pairs = microservice.stream()
                .flatMap(i -> externalClasses.stream()
                        .map(j -> Pair.of(i, j)))
                .collect(Collectors.toList());

        // Calculate the metric
        var result = pairs.stream()
                .mapToDouble(pair -> data(pair.getKey(), pair.getValue(), nbDataManipulatedInMicro))
                .sum() / pairs.size();
        clustering.getOptimizationData().getFInter().put(optimizationKey, result);
        return result;
    }
}
