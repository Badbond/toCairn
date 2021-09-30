package me.soels.tocairn.solver.metric;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import me.soels.tocairn.model.DependenceRelationship;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import me.soels.tocairn.solver.OptimizationData;
import org.apache.commons.lang3.tuple.Pair;

/**
 * FOne metric as proposed by Selmadji et al. (2020).
 * <p>
 * This metric should be maximized but, as the MOEA framework only allows for minimization objectives, we negate the
 * value.
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFOne extends SelmadjiStructuralBehavior {
    @Override
    public double calculate(Clustering clustering) {
        // Perform the FOne metric
        return -1 * clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> calculateMicroservice(microservice, clustering))
                .sum();
    }

    private double calculateMicroservice(Set<OtherClass> microservice, Clustering clustering) {
        var optimizationKey = microservice.stream().map(clazz -> clazz.getId().toString()).sorted().collect(Collectors.joining(""));
        var existingValue = clustering.getOptimizationData().getFOne().get(optimizationKey);
        if (existingValue != null) {
            return existingValue;
        }

        var result = 0.5 * (interCoup(microservice, clustering.getOptimizationData()) + interCoh(microservice));
        clustering.getOptimizationData().getFOne().put(optimizationKey, result);
        return result;
    }

    /**
     * Calculates interCoup as devised by Selmadji et al. (2020).
     *
     * @param microservice     the microservice to perform the measurement for
     * @param optimizationData optimization data from previous calculations
     * @return the interCoup value for this microservice
     */
    private double interCoup(Set<OtherClass> microservice, OptimizationData optimizationData) {
        var coupValues = microservice.stream()
                // Get all the pairs of classes in this microservice excluding self-pairs.
                .flatMap(i -> microservice.stream()
                        .filter(j -> !i.equals(j))
                        .map(j -> Pair.of(i, j)))
                // Calculate pair coup values
                .map(pair -> coup(pair.getKey(), pair.getValue(), optimizationData.getNbTotalCalls()))
                .collect(Collectors.toList());

        if (coupValues.stream().allMatch(Objects::isNull)) {
            // We do not have any pairs in this microservice to calculate this metric with.
            // We set the value to 0.0 to favour microservices with at least two classes.
            coupValues.add(0.0);
        }

        // Calculate the standard deviation of these coup values
        double sigma = getSigma(coupValues);

        // Perform the interCoup measurement
        return (coupValues.stream().filter(Objects::nonNull).mapToDouble(value -> value).sum() - sigma) / coupValues.size();
    }

    /**
     * Calculates the interCoh measurement as devised by Selmadji et al. (2020).
     * <p>
     * Here, the amount of direct connections are method calls, references and construct calls actually made where
     * the possible connections are those of all methods defined in all classes in this microservice.
     *
     * @param microservice the classes in the microservice
     * @return the inter cohesion value for this microservice
     */
    private double interCoh(Set<OtherClass> microservice) {
        var nbDirectConnections = microservice.stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                .filter(dep -> microservice.contains(dep.getCallee()))
                .mapToInt(DependenceRelationship::getConnections)
                .sum();
        var nbPossibleConnections = microservice.stream()
                .mapToInt(OtherClass::getMethodCount)
                .sum() * (microservice.size() - 1); // Minus one to exclude self-reference.
        if (nbPossibleConnections <= 0) {
            // Only one class in microservice, we want to penalize this and therefore set cohesion to 0.
            return 0;
        }
        return nbDirectConnections / (double) nbPossibleConnections;
    }
}
