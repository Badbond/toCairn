package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        return -1 * clustering.getByCluster().values().stream()
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
    private double overhead(List<OtherClass> microservice, Set<OtherClass> allClasses, double averageSize) {
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

        var sum = 0;
        var dep = maybeDep.get();

        // Measure size shared with every method call
        for (var methodCall : dep.getMethodCalls().entrySet()) {
            for (var method : dep.getCallee().getMethods().entrySet()) {
                if (method.getKey().equals(methodCall.getKey())) {
                    // Found matching method of method call with FQN in callee, retrieve the size of its parameters
                    var parameterSize = sizeOf(method, allClasses, averageSize);
                    // Increase the sum with the parameter's size multiplied by the dynamic frequency
                    sum += parameterSize * methodCall.getValue();
                }
            }
        }

        return sum;
    }

    /**
     * Measures the size of the data shared in the given method call.
     *
     * @param method      the method called
     * @param allClasses  all the classes containing size definitions
     * @param averageSize the average measured size of the classes in the application
     * @return the size of the method call
     */
    private double sizeOf(Map.Entry<String, List<String>> method, Set<OtherClass> allClasses, double averageSize) {
        var parameterSize = 0.0;
        for (var parameter : method.getValue()) {
            if (isPrimitive(parameter)) {
                parameterSize += 4; // Most primitives are allocated with 32 bits = 8 bytes.
                continue;
            }
            // We have an object which we match against known classes or default to the average size otherwise.
            parameterSize += allClasses.stream()
                    .filter(clazz -> clazz.getIdentifier().equals(parameter))
                    .findFirst()
                    .map(AbstractClass::getSize)
                    .map(Double::valueOf)
                    .orElse(averageSize);
        }
        return parameterSize;
    }

    /**
     * Returns whether the class identified by the FQN is a (boxed) primitive class or not.
     *
     * @param fqn the class to check
     * @return whether the class is a (boxed) primitive or not
     */
    private boolean isPrimitive(String fqn) {
        // TODO: Return whether the FQN is a (boxed) Java primitive or not.
        return fqn.length() % 2 == 0;
    }
}
