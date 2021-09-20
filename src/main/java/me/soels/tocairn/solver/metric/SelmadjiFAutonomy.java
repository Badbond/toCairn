package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        var nbTotalCalls = clustering.getNbTotalCalls();
        return clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> exterCoup(microservice, clustering, nbTotalCalls))
                .sum();
    }

    /**
     * Calculates exterCoup measurement as devised by Selmadji et al. (2020).
     *
     * @param microservice the microservice to perform the measurement for
     * @param clustering   the clustering to retrieve external pairs from
     * @param nbTotalCalls the total amount of method calls made within the application
     * @return the exterCoup value for this microservice
     */
    private double exterCoup(List<OtherClass> microservice, Clustering clustering, long nbTotalCalls) {
        // Get all the classes that do not belong to this microservice.
        var externalClasses = clustering.getByClass().keySet().stream()
                .filter(clazz -> !microservice.contains(clazz))
                .collect(Collectors.toList());

        // Get all the pairs of classes in this microservice with those of external classes and calculate its coup value
        var coupValues = microservice.stream()
                .flatMap(i -> externalClasses.stream()
                        .map(j -> Pair.of(i, j)))
                .map(pair -> coup(pair.getKey(), pair.getValue(), nbTotalCalls))
                .collect(Collectors.toList());

        if (coupValues.stream().allMatch(Objects::isNull)) {
            // This microservice is not coupled with any other microservice.
            // Return 0. We do not set 0 values in coup() as that would skew the deviation in case we do have at
            // least one coupling pair.
            return 0.0;
        }

        // Calculate the standard deviation of these coup values
        double sigma = getSigma(coupValues);

        // Perform the exterCoup measurement
        return (coupValues.stream().filter(Objects::nonNull).mapToDouble(value -> value).sum() - sigma) / coupValues.size();
    }
}
