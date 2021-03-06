package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.DependenceRelationship;
import me.soels.tocairn.model.OtherClass;

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
     *
     * @param i            the first class
     * @param j            the second class
     * @param nbTotalCalls the total amount of method calls within the application
     * @return the coup value
     */
    protected Double coup(OtherClass i, OtherClass j, long nbTotalCalls) {
        var toJ = getNbCalls(i, j);
        var toI = getNbCalls(j, i);
        return (toI + toJ) / (double) nbTotalCalls;
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
}
