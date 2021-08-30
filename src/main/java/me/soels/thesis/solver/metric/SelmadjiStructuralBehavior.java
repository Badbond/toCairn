package me.soels.thesis.solver.metric;

import me.soels.thesis.model.DependenceRelationship;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.solver.Clustering;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Shared logic for the {@code FStructBeh} metric devised by Selmadji et al. (2020).
 * <p>
 * Based on the metric and its definition as proposed by Selmadji et al. (2020). This class also contains metrics shared
 * between both {@link SelmadjiFOne} and {@link SelmadjiFAutonomy}.
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 *
 * @see SelmadjiFAutonomy
 * @see SelmadjiFOne
 */
public abstract class SelmadjiStructuralBehavior implements Metric {
    /**
     * Returns the {@code coup} value between two classes.
     * <p>
     * Returns {@code null} when these two classes do not have a connection with each other s.t. they can be filtered
     * out from average calculations.
     *
     * @param i            the first class
     * @param j            the second class
     * @param totalNbCalls the total amount of method calls within the application
     * @return the coup value
     */
    protected Double coup(OtherClass i, OtherClass j, long totalNbCalls) {
        var toJ = getNbCalls(i, j);
        var toI = getNbCalls(j, i);

        if (toI == 0 && toJ == 0) {
            return null;
        } else {
            return (toI + toJ) / (double) totalNbCalls;
        }
    }

    /**
     * Get the amount of calls made from class {@code a} to class {@code b}
     *
     * @param a the caller class
     * @param b the callee class
     * @return the amount of calls made from a to b
     */
    private Integer getNbCalls(OtherClass a, OtherClass b) {
        return a.getDependenceRelationships().stream()
                .filter(dep -> dep.getCallee().equals(b))
                .findFirst()
                .map(DependenceRelationship::getStaticFrequency)
                .orElse(0);
    }

    /**
     * Returns the sigma of a list of (coup) values.
     * <p>
     * The given double list is allowed to have {@code null} values which will be filtered out. With that, the input
     * list can still be used to calculate the number of possible pairs.
     *
     * @param allValues all values
     * @return the sigma of all values
     */
    protected double getSigma(List<Double> allValues) {
        var all = allValues.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Based on https://stackoverflow.com/a/14839593
        var n = all.size();
        var sum = all.stream().mapToDouble(value -> value).sum();
        var sqSum = all.stream().mapToDouble(value -> pow(value, 2)).sum();
        var mean = sum / n;
        var variance = sqSum / n - mean * mean;
        return sqrt(variance);
    }

    /**
     * Returns the total number of calls made in the application between {@link OtherClass}.
     *
     * @param clustering the clustering structure
     * @return the total amount of calls made in the application
     */
    protected long getTotalNbCalls(Clustering clustering) {
        return clustering.getByClass().keySet().stream()
                .flatMap(clazz -> clazz.getDependenceRelationships().stream())
                .mapToLong(DependenceRelationship::getStaticFrequency)
                .sum();
    }
}
