package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.SolverConfiguration;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class that performs the metric calculation, allows for normalizing the metric values and calculates the
 * quality based on the (weighted) metrics.
 */
public class QualityCalculator {
    private QualityCalculator() {
        // Utility class, do not initialise.
    }


    /**
     * Performs the metrics for the given {@link Clustering}.
     *
     * @param clustering the clustering to perform the metrics for
     * @return a data structure containing the resulting metric values
     */
    public static Map<MetricType, double[]> performMetrics(SolverConfiguration configuration, Clustering clustering) {
        Map<MetricType, double[]> result = new EnumMap<>(MetricType.class);
        for (var metricType : configuration.getMetrics()) {
            var metricValues = new double[metricType.getMetrics().size()];
            for (int i = 0; i < metricType.getMetrics().size(); i++) {
                metricValues[i] = metricType.getMetrics().get(i).calculate(clustering);
            }
            result.put(metricType, metricValues);
        }
        return result;
    }

    /**
     * Returns the weighted total quality for the metrics given.
     * <p>
     * The metrics may or may not be normalized.
     *
     * @param metrics the metrics to calculate the total quality from
     * @param weights the weights of the metrics
     * @return the weighted total quality
     */
    public static double getWeightedTotalQuality(Map<MetricType, double[]> metrics, List<Double> weights) {
        var metricsArray = metrics.values().stream()
                .flatMapToDouble(Arrays::stream)
                .toArray();

        var quality = 0.0;
        for (int i = 0; i < weights.size(); i++) {
            var weight = weights.get(i);
            var metric = metricsArray[i];
            quality += metric * weight;
        }
        return quality / weights.stream().mapToDouble(v -> v).sum();
    }

    /**
     * Returns the minimum and maximum values for the given metric values such that they can be used in normalization.
     *
     * @param allMetricValues all the metric values
     * @return the minimum and maximum values for the metrics
     * @see <a href="https://people.revoledu.com/kardi/tutorial/Similarity/Normalization.html">Normalization</a>
     */
    @SuppressWarnings("unchecked") // Generic array creation not possible, safe cast.
    public static Map<MetricType, Pair<Double, Double>[]> getMinMaxValues(List<Map<MetricType, double[]>> allMetricValues) {
        // Initialize with one solution
        var minMaxValues = allMetricValues.get(0).entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(),
                        (Pair<Double, Double>[]) Arrays.stream(entry.getValue())
                                .mapToObj(d -> Pair.of(d, d))
                                .toArray(Pair[]::new)
                ))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        // Calculate min-max pair for every metric.
        for (var metricValues : allMetricValues) {
            for (var entry : metricValues.entrySet()) {
                var metricType = entry.getKey();
                for (int i = 0; i < entry.getValue().length; i++) {
                    var metricValue = entry.getValue()[i];
                    var current = minMaxValues.get(metricType)[i];
                    if (metricValue < current.getKey()) {
                        current = Pair.of(metricValue, current.getValue());
                    }
                    if (metricValue > current.getValue()) {
                        current = Pair.of(current.getKey(), metricValue);
                    }
                    minMaxValues.get(metricType)[i] = current;
                }
            }
        }
        return minMaxValues;
    }

    /**
     * Normalizes the given metrics based on the given metrics' minimum and maximum values.
     * <p>
     * Negative metrics are first offset such that they will start from 0.0. Then, the metric will be mapped
     * to range from [0,1] based on the provided minimum and maximum values. If both minimum and maximum is the same
     * value, we cap the value to a maximum of 1.0 such that either quality is considered the same (even though might
     * be improved) when above 1.0 but at least allows room for improvement to under 1.0.
     * <p>
     * It is recommended to, when possible, normalize over the entire analysis instead of parts of the analysis.
     * It is hard to compare normalized metrics from different parts of an analysis. For this, one should compare
     * the non-normalized metrics.
     * <p>
     * Note that the order of the minMax list should match those of the metrics.
     *
     * @param metrics  the metrics to normalize
     * @param minMaxes the minimum and maximum value for every metric
     * @return the normalized metrics
     */
    public static Map<MetricType, double[]> normalize(Map<MetricType, double[]> metrics,
                                                      Map<MetricType, Pair<Double, Double>[]> minMaxes) {
        var normalizedMetrics = new EnumMap<MetricType, double[]>(MetricType.class);
        for (var entry : metrics.entrySet()) {
            double[] normalized = new double[entry.getValue().length];
            for (int i = 0; i < entry.getValue().length; i++) {
                var knownRange = minMaxes.get(entry.getKey())[i];
                var currentMetric = entry.getValue()[i];
                if (knownRange.getKey() < 0) {
                    // Transform by adding the absolute value of minValue to the metrics
                    var shift = Math.abs(knownRange.getKey());
                    currentMetric += shift;
                    knownRange = Pair.of(0.0, knownRange.getValue() + shift);
                }
                var division = knownRange.getValue() - knownRange.getKey();
                if (division == 0) {
                    // Here all metrics have the same value (in the space of N0). We cap the value to a maximum of 1.0
                    // such that either quality is considered the same (even though might be improved) when above 1.0
                    // but at least allows room for improvement to under 1.0.
                    normalized[i] = Math.min(currentMetric, 1.0);
                } else {
                    normalized[i] = (currentMetric - knownRange.getKey()) / division;
                }
            }
            normalizedMetrics.put(entry.getKey(), normalized);
        }
        return normalizedMetrics;
    }
}
