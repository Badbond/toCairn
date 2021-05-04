package me.soels.tocairn.solver.ahca;

import me.soels.tocairn.model.*;
import me.soels.tocairn.solver.Clustering;
import me.soels.tocairn.solver.ClusteringBuilder;
import me.soels.tocairn.solver.metric.QualityCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class that produces initial state for the {@link AHCASolver}.
 */
public class AHCAInitializer {
    private AHCAInitializer() {
        // Utility class, do not initialise.
    }

    /**
     * Returns a map representing the connected {@link OtherClass} for one {@link OtherClass}.
     * <p>
     * This not only contains those defined by the class' {@link OtherClass#getDependenceRelationships()}, but also
     * those linked through (recursive) {@link OtherClass#getDataRelationships()}. This recursiveness is based
     * on both incoming and outgoing data dependencies.
     *
     * @return the map representing the connectedness of classes to cluster
     */
    static Map<OtherClass, List<OtherClass>> calculateClassConnectedness(EvaluationInput input) {
        Map<OtherClass, List<OtherClass>> result = input.getOtherClasses().stream()
                .collect(Collectors.toMap(dataClass -> dataClass, v -> new ArrayList<>()));
        Map<DataClass, List<OtherClass>> dataConnections = input.getDataClasses().stream()
                .collect(Collectors.toMap(dataClass -> dataClass, v -> new ArrayList<>()));
        Map<DataClass, List<DataClass>> relatedDataClasses = input.getDataClasses().stream()
                .collect(Collectors.toMap(dataClass -> dataClass, v -> new ArrayList<>()));

        // Direct relationships
        input.getOtherClasses().forEach(clazz ->
                result.get(clazz).addAll(clazz.getDependenceRelationships().stream()
                        .map(DependenceRelationship::getCallee)
                        .map(OtherClass.class::cast) // Guaranteed by relationship from originating other class
                        .collect(Collectors.toList()))
        );

        // Add incoming relationships to data classes
        input.getOtherClasses().forEach(otherClass ->
                otherClass.getDataRelationships().stream()
                        .map(DataRelationship::getCallee)
                        .forEach(dataClass -> dataConnections.get(dataClass).add(otherClass)));

        // Add outgoing relationships to data classes
        input.getDataClasses().forEach(dataClass ->
                dataClass.getDependenceRelationships().stream()
                        .map(DependenceRelationship::getCallee)
                        .filter(OtherClass.class::isInstance)
                        .map(OtherClass.class::cast)
                        .forEach(otherClass -> dataConnections.get(dataClass).add(otherClass)));

        // Add data class to data class relationships
        input.getDataClasses().forEach(dataClass ->
                dataClass.getDependenceRelationships().stream()
                        .map(DependenceRelationship::getCallee)
                        .filter(DataClass.class::isInstance)
                        .map(DataClass.class::cast)
                        .forEach(dataClass2 -> {
                            relatedDataClasses.get(dataClass).add(dataClass2);
                            relatedDataClasses.get(dataClass2).add(dataClass);
                        }));

        // Add other class to other class relationships from (recursive) data relationship classes
        input.getDataClasses().stream()
                .flatMap(dataClass -> getConnectedDataClasses(dataClass, relatedDataClasses).stream()
                        .map(dataConnections::get))
                .forEach(connectedOtherClasses -> connectedOtherClasses.forEach(
                        otherClass -> result.get(otherClass).addAll(connectedOtherClasses.stream()
                                // Make sure that we don't interpret an edge to itself.
                                .filter(connectedClass -> !connectedClass.equals(otherClass))
                                .collect(Collectors.toList()))));

        return result;
    }

    /**
     * Returns the set of data classes that the given starting data class is implicitly and recursively connected to.
     *
     * @param startClass         the data class to define the connected data classes for
     * @param relatedDataClasses the structure of connected data classes
     * @return the data classes connected to the given data class
     */
    static Set<DataClass> getConnectedDataClasses(DataClass startClass, Map<DataClass, List<DataClass>> relatedDataClasses) {
        var seen = new HashSet<DataClass>();
        var toVisit = new ArrayDeque<>(Set.of(startClass));
        while (!toVisit.isEmpty()) {
            var dataClass = toVisit.pop();
            if (seen.contains(dataClass)) {
                continue;
            }

            seen.add(dataClass);
            toVisit.addAll(relatedDataClasses.get(dataClass));
        }
        return seen;
    }


    /**
     * Creates the initial solution for the AHCA where every class is placed in its own cluster.
     * <p>
     * We also calculate the metrics for this solution such that it can be used in analysis when the user has not
     * specified the number of desired clusters.
     *
     * @param configuration the configuration for the AHCA.
     * @param input         the class dependency graph
     * @return the initial clustering solution
     */
    static AHCAClustering createInitialSolution(AHCAConfiguration configuration, EvaluationInput input) {
        var builder = new ClusteringBuilder();
        for (int i = 0; i < input.getOtherClasses().size(); i++) {
            builder.addToCluster(input.getOtherClasses().get(i), i);
        }
        var clustering = builder.build();

        var solution = new Solution();
        solution.setMicroservices(clustering.getByCluster().entrySet().stream()
                .map(entry -> new Microservice(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return createClustering(configuration, solution, clustering);
    }

    /**
     * Creates a clustering based on the given solution.
     *
     * @param solution the solution to create a clustering for
     * @return the AHCA clustering
     */
    static AHCAClustering createClusteringFromSolution(AHCAConfiguration configuration, Solution solution) {
        var clustering = new ClusteringBuilder(solution).build();
        return createClustering(configuration, solution, clustering);
    }

    private static AHCAClustering createClustering(AHCAConfiguration configuration, Solution solution, Clustering clustering) {
        solution.setMetricValues(QualityCalculator.performMetrics(configuration, clustering));
        // Note, we do not do normalization of the metrics in this step as there is only one clustering in initialization.
        // This would cause all metrics to be 1.
        var quality = QualityCalculator.getWeightedTotalQuality(solution.getMetricValues(), configuration.getWeights());
        return new AHCAClustering(clustering, solution, quality, quality);
    }
}
