package me.soels.thesis.model;

import me.soels.thesis.solver.metric.Metric;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static me.soels.thesis.util.GenericCollectionExtractor.extractType;

/**
 * The input for the multi-objective evolutionary algorithm.
 * <p>
 * This input is to be constructed from initial analysis of the application that we wish to identify microservice
 * boundaries for. It has to contain all the necessary information to calculate the fitness of the clustering generated
 * by the algorithm for the given objective function.
 *
 * @see EvaluationInputBuilder
 * @see Metric
 */
public final class EvaluationInput {
    private final List<? extends AbstractClass> classes;

    EvaluationInput(List<? extends AbstractClass> classes) {
        classes.sort(Comparator.comparing(AbstractClass::getIdentifier));
        this.classes = Collections.unmodifiableList(classes);
    }

    /**
     * Returns all the classes in the project.
     *
     * @return all the classes in the project
     */
    public List<AbstractClass> getClasses() {
        return classes.stream()
                .map(AbstractClass.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the non-data classes (e.g. service classes, libraries) of the project.
     *
     * @return the non-data classes of the project
     */
    public List<OtherClass> getOtherClasses() {
        return extractType(classes, OtherClass.class);
    }

    /**
     * Returns the data classes of the project.
     *
     * @return the data classes of the project
     */
    public List<DataClass> getDataClasses() {
        return extractType(classes, DataClass.class);
    }
}
