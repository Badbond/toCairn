package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;
import java.util.stream.Collectors;

import static me.soels.tocairn.util.SigmaCalculator.getSigma;

/**
 * FAutonomy metric as proposed by Selmadji et al. (2020).
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFAutonomy extends SelmadjiStructuralBehavior {
    @Override
    public double calculate(Clustering clustering) {
        return clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> exterCoup(microservice, clustering))
                .average()
                .orElse(0);
    }

    /**
     * Calculates exterCoup measurement as devised by Selmadji et al. (2020).
     *
     * @param microservice the microservice to perform the measurement for
     * @param clustering   the clustering to retrieve external pairs from
     * @return the exterCoup value for this microservice
     */
    private double exterCoup(Set<OtherClass> microservice, Clustering clustering) {
        var optimizationKey = microservice.stream().map(clazz -> clazz.getId().toString()).sorted().collect(Collectors.joining(""));
        var existingValue = clustering.getOptimizationData().getFAutonomy().get(optimizationKey);
        if (existingValue != null) {
            return existingValue;
        }

        // Get all the classes that do not belong to this microservice.
        var externalClasses = clustering.getByClass().keySet().stream()
                .filter(clazz -> !microservice.contains(clazz))
                .collect(Collectors.toList());

        // Get all the pairs of classes in this microservice with those of external classes and calculate its coup value
        var coupValues = microservice.stream()
                .flatMap(i -> externalClasses.stream()
                        .map(j -> Pair.of(i, j)))
                .map(pair -> coup(pair.getKey(), pair.getValue(), clustering.getOptimizationData().getNbTotalCalls()))
                .collect(Collectors.toList());

        // Perform the exterCoup measurement
        var coupSum = coupValues.stream().mapToDouble(value -> value).sum();
        var result = (coupSum - getSigma(coupValues)) / coupValues.size();
        clustering.getOptimizationData().getFAutonomy().put(optimizationKey, result);
        return result;
    }
}
