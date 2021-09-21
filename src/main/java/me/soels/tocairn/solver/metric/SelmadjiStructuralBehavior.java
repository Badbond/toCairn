package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.DependenceRelationship;
import me.soels.tocairn.model.OtherClass;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * @param nbTotalCalls the total amount of method calls within the application
     * @return the coup value
     */
    protected Double coup(OtherClass i, OtherClass j, long nbTotalCalls) {
        var toJ = getNbCalls(i, j);
        var toI = getNbCalls(j, i);

        if (toI == 0 && toJ == 0) {
            return null;
        } else {
            return (toI + toJ) / (double) nbTotalCalls;
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

        // Based on https://stackoverflow.com/a/14839142 and https://stackoverflow.com/a/14839593
        var sum = all.stream().mapToDouble(value -> value).sum();
        var n = all.size();
        var mean = sum / n;
        var sd = 0.0;
        for (var number : all) {
            sd += Math.pow(number - mean, 2);
        }
        var result = sqrt(sd / (n - 1));
        if (Double.valueOf(result).isNaN()) {
            // Happens when n = 1 (division by 0)
            return 0.0;
        } else {
            return result;
        }
    }


}
