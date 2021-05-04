package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;
import java.util.stream.Collectors;

import static me.soels.tocairn.util.Constants.PRIMITIVE_STRING;

/**
 * Overhead measurement by Carvalho et al. (2020).
 * <p>
 * They calculate this metric on a method-unit instead of class-unit. Therefore, they measure how much data is shared
 * between methods. Instead, we measure how much data is shared between classes by summarizing over data shared
 * between all method calls in a class.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE Transactions on software engineering, 20</i>(6), 476-493.'.
 **/
public class CarvalhoOverhead implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        var averageSize = clustering.getByClass().keySet().stream()
                .mapToLong(AbstractClass::getSize)
                .average()
                .orElseThrow(() -> new IllegalArgumentException("Could not calculate average size of classes"));

        return clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> overhead(microservice, clustering.getByClass().keySet(), averageSize))
                .sum();
    }

    /**
     * Measures the total overhead introduced by the microservice
     *
     * @param microservice the classes identifying the suggested microservice boundary
     * @param allClasses   all the classes containing size definitions
     * @param averageSize  the average measured size of the classes in the application
     * @return the total overhead introduced by this microservice
     */
    private double overhead(Set<OtherClass> microservice, Set<OtherClass> allClasses, double averageSize) {
        // Get all the classes that do not belong to this microservice.
        var externalClasses = allClasses.stream()
                .filter(clazz -> !microservice.contains(clazz))
                .collect(Collectors.toList());
        // Get all the pairs of classes in this microservice with those of external classes.
        var pairs = microservice.stream()
                .flatMap(i -> externalClasses.stream()
                        .map(j -> Pair.of(i, j)))
                .collect(Collectors.toList());
        return pairs.stream()
                .mapToDouble(pair -> dt(pair.getKey(), pair.getValue(), allClasses, averageSize))
                .sum();
    }

    /**
     * Measures the data transferred in method calls from class {@code i} to class {@code j}.
     *
     * @param i           the caller
     * @param j           the callee
     * @param allClasses  all the classes containing size definitions
     * @param averageSize the average measured size of the classes in the application
     * @return the data transferred from class i to class j
     */
    private double dt(OtherClass i, OtherClass j, Set<OtherClass> allClasses, double averageSize) {
        var maybeDep = i.getDependenceRelationships().stream()
                .filter(dep -> dep.getCallee().equals(j))
                .findFirst();
        if (maybeDep.isEmpty()) {
            // No relationship, therefore no data is shared.
            return 0;
        }

        // Increase sum based size of classes shared in this dependency times how often they are shared
        return maybeDep.get().getSharedClasses().entrySet().stream()
                .mapToDouble(classShared -> sizeOf(classShared.getKey(), allClasses, averageSize) * classShared.getValue())
                .sum();
    }

    /**
     * Measures the size of the class shared in this dependency.
     *
     * @param classShared the class shared for which to determine the size of
     * @param allClasses  all the classes containing size definitions
     * @param averageSize the average measured size of the classes in the application
     * @return the size of the method call
     */
    private double sizeOf(String classShared, Set<OtherClass> allClasses, double averageSize) {
        if (classShared.equals(PRIMITIVE_STRING)) {
            return 4; // Most primitives are allocated with 32 bits = 8 bytes.
        } else {
            return allClasses.stream()
                    .filter(clazz -> clazz.getIdentifier().equals(classShared))
                    .findFirst()
                    .map(AbstractClass::getSize)
                    .map(Double::valueOf)
                    .orElse(averageSize);
        }
    }
}
