package me.soels.thesis.solver.metric;

import me.soels.thesis.model.DataRelationship;
import me.soels.thesis.model.OtherClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static me.soels.thesis.model.DataRelationshipType.WRITE;

/**
 * Functional interface to identify a metric for the {@link MetricType#DATA_AUTONOMY} objective.
 * <p>
 * Based on the metric and its definition as proposed by Selmadji et al. (2020). This class also contains metrics shared
 * between both {@link SelmadjiFInter} and {@link SelmadjiFIntra}.
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 *
 * @see SelmadjiFIntra
 * @see SelmadjiFInter
 */
public abstract class SelmadjiDataAutonomy implements Metric {
    /**
     * Performs the DataDepends metric as described in the thesis. Note that as we are using already existing
     * relationships, we omit the {@code 0} case.
     *
     * @param i the data relationship from otherClass i to d
     * @param j the data relationship from otherClass j to d
     * @return the measurement of data dependence of two classes to a shared data class
     */
    private double dataDepends(DataRelationship i, DataRelationship j) {
        if (i.getType() == WRITE && j.getType() == WRITE) {
            return 1.0;
        } else if (i.getType() == WRITE || j.getType() == WRITE) {
            return 0.5;
        } else {
            return 0.25;
        }
    }

    /**
     * Performs the Data metric as described in the thesis.
     *
     * @param i                        the first class to match data relationship for
     * @param j                        the second class to match data relationship for
     * @param nbDataManipulatedInMicro the number of data classes manipulated by ms
     * @return the measurement of data dependence of two classes to its shared data class
     */
    protected double data(OtherClass i, OtherClass j, long nbDataManipulatedInMicro) {
        var sharedDataRelationshipPairs = new ArrayList<Pair<DataRelationship, DataRelationship>>();
        for (var dataRelI : i.getDataRelationships()) {
            j.getDataRelationships().stream()
                    .filter(dataRelJ -> dataRelJ.getCallee().equals(dataRelI.getCallee()))
                    .findFirst()
                    .ifPresent(shared -> sharedDataRelationshipPairs.add(Pair.of(dataRelI, shared)));
        }

        if (sharedDataRelationshipPairs.isEmpty()) {
            // These two classes do not share a data object, returning 0. Corresponding to the sum of k in Data of Selmadji.
            return 0.0;
        }

        return sharedDataRelationshipPairs.stream()
                .mapToDouble(pair -> dataDepends(pair.getKey(), pair.getValue()) * freq(pair.getKey(), pair.getValue()))
                .sum() / nbDataManipulatedInMicro;
    }

    /**
     * Calculates the frequency at which two classes interact with a data class.
     * <p>
     * If dynamic information is present, we will add that frequency to that of the static frequency.
     *
     * @param i the relationship from other class i to d
     * @param j the relationship from other class j to d
     * @return the frequency metric
     */
    private double freq(DataRelationship i, DataRelationship j) {
        var freqI = i.getStaticFrequency() + i.getDynamicFrequency().orElse(0);
        var freqJ = j.getStaticFrequency() + j.getDynamicFrequency().orElse(0);

        // Based on https://stackoverflow.com/a/14839593
        var sum = freqI + freqJ;
        var sqSum = pow(freqI, 2) + pow(freqJ, 2);
        var mean = sum / (double) 2;
        var variance = sqSum / 2 - mean * mean;
        return freqI + freqJ - sqrt(variance);
    }

    /**
     * Returns the number of unique data classes modified by the classes representing the microservice.
     *
     * @param classes the classes representing the microservice
     * @return the amount of data classes linked to the classes
     */
    protected long nbDataManipulatedInMicro(List<OtherClass> classes) {
        return classes.stream()
                .flatMap(clazz -> clazz.getDataRelationships().stream())
                .map(DataRelationship::getCallee)
                .distinct()
                .count();
    }
}
