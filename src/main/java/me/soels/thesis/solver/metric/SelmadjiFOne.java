package me.soels.thesis.solver.metric;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FOne metric as proposed by Selmadji et al. (2020).
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFOne extends SelmadjiStructuralBehavior {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        // Calculate total calls first as these are based on the entire application.
        var nbTotalCalls = getTotalNbCalls(clustering);

        // Perform the FOne metric
        return clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> 0.5 * (interCoup(microservice, nbTotalCalls) + interCoh(microservice)))
                .sum();
    }

    /**
     * Calculates interCoup as devised by Selmadji et al. (2020).
     *
     * @param microservice the microservice to perform the measurement for
     * @param totalNbCalls the total amount of method calls made within the application
     * @return the interCoup value for this microservice
     */
    private double interCoup(List<OtherClass> microservice, long totalNbCalls) {
        var coupValues = microservice.stream()
                // Get all the pairs of classes in this microservice excluding self-pairs.
                .flatMap(i -> microservice.stream()
                        .filter(j -> !i.equals(j))
                        .map(j -> Pair.of(i, j)))
                // Calculate pair coup values
                .map(pair -> coup(pair.getKey(), pair.getValue(), totalNbCalls))
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
     *
     * @param microservice the classes in the microservice
     * @return the inter cohesion value for this microservice
     */
    private double interCoh(List<OtherClass> microservice) {
        // TODO: Extract method information and use here.
        var nbDirectConnections = 1;
        var nbPossibleConnections = 1;
        return nbDirectConnections / nbPossibleConnections;
    }
}
