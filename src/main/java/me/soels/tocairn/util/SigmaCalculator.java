package me.soels.tocairn.util;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Math.sqrt;

public class SigmaCalculator {
    private SigmaCalculator() {
        // Utility class, do not instantiate
    }

    /**
     * Returns the sigma of a list of values.
     * <p>
     * The given double list is allowed to have {@code null} values which will be filtered out. With that, the input
     * list can still be used to calculate the number of possible pairs.
     *
     * @param allValues all values
     * @return the sigma of all values
     */
    public static double getSigma(List<? extends Number> allValues) {
        var all = allValues.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (all.size() <= 1) {
            // Value of 0 or 1 would cause division by 0.
            return 0.0;
        }


        // Based on https://stackoverflow.com/a/14839142 and https://stackoverflow.com/a/14839593
        var sum = all.stream().mapToDouble(Number::doubleValue).sum();
        var n = all.size();
        var mean = sum / n;
        var sd = 0.0;
        for (var number : all) {
            sd += Math.pow(number.doubleValue() - mean, 2);
        }
        var result = sqrt(sd / (n - 1));
        if (Double.valueOf(result).isNaN()) {
            // Fallback.
            return 0.0;
        } else {
            return result;
        }
    }
}
