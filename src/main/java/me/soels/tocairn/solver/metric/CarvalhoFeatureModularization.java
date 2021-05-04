package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature modularization measurement by Carvalho et al. (2020).
 * <p>
 * They calculate this metric on a method-unit instead of class-unit. Therefore, they measure how much methods in a
 * microservice implement features. Instead, we measure how much classes in a microservice implement features.
 * <p>
 * This metric should be maximized but, as the MOEA framework only allows for minimization objectives, we negate the
 * value.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE Transactions on software engineering, 20</i>(6), 476-493.'.
 **/
public class CarvalhoFeatureModularization implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        var featureModularizationMicroservice = clustering.getByCluster().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), featureModularization(entry.getValue())))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        var uniquekeys = featureModularizationMicroservice.values().stream()
                .map(Pair::getKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return -1 * clustering.getByCluster().keySet().stream()
                .mapToDouble(microserviceId ->
                        featureModularizationMicroservice.get(microserviceId).getValue() +
                                uniquekeys.size() / (double) clustering.getByCluster().size())
                .sum();
    }

    /**
     * Measures the feature modularization for a microservice.
     * <p>
     * Returns both the feature modularization value as per {@code f(MSc)} by Carvalho et al. as well as returning the
     * label of the predominant feature to be used in determining {@code FMSA} devised by Carvalho et al.
     *
     * @param microservice the classes identifying the suggested microservice boundary
     * @return the feature modularization and predominant feature for this microservice
     */
    private Pair<String, Double> featureModularization(Set<OtherClass> microservice) {
        var msaFeatureSet = getFeatureSet(microservice);

        var predominantFeature = msaFeatureSet.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));

        if (predominantFeature.isEmpty()) {
            return null;
        }

        return Pair.of(predominantFeature.get().getKey(), predominantFeature.get().getValue() /
                (double) msaFeatureSet.values().stream().mapToInt(value -> value).sum());
    }

    private Map<String, Integer> getFeatureSet(Set<OtherClass> microservice) {
        var result = new HashMap<String, Integer>();
        microservice.stream()
                .flatMap(clazz -> clazz.getFeatures().stream())
                .forEach(feature -> result.compute(feature, (f, counter) -> counter == null ? 1 : counter + 1));
        return result;
    }
}
