package me.soels.thesis.model;

import me.soels.thesis.objectives.Objective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The input for the multi-objective evolutionary algorithm.
 * <p>
 * This input is to be constructed from initial analysis of the application that we wish to identify microservice
 * boundaries for. It has to contain all the necessary information to calculate the fitness of the clustering generated
 * by the algorithm for the given objective function.
 * <p>
 * The data in this class is to be immutable so that it can be shared among solution evaluations.
 *
 * @see AnalysisModelBuilder
 * @see Objective
 */
public final class AnalysisModel {
    private final List<AbstractClass> allClasses;
    private final List<OtherClass> otherClasses;
    private final List<DataClass> dataClasses;
    private final List<DataRelationship> dataRelations;
    private final List<DependenceRelationship> dependencies;

    AnalysisModel(List<OtherClass> otherClasses,
                  List<DataClass> dataClasses,
                  List<DependenceRelationship> dependencies,
                  List<DataRelationship> dataRelationships) {
        var allClasses = new ArrayList<AbstractClass>(otherClasses);
        allClasses.addAll(dataClasses);

        this.allClasses = Collections.unmodifiableList(allClasses);
        this.dataClasses = Collections.unmodifiableList(dataClasses);
        this.otherClasses = Collections.unmodifiableList(otherClasses);
        this.dependencies = Collections.unmodifiableList(dependencies);
        this.dataRelations = Collections.unmodifiableList(dataRelationships);
    }

    public List<AbstractClass> getAllClasses() {
        return allClasses;
    }

    public List<OtherClass> getOtherClasses() {
        return otherClasses;
    }

    public List<DataClass> getDataClasses() {
        return dataClasses;
    }

    public List<DataRelationship> getDataRelations() {
        return dataRelations;
    }

    public List<DependenceRelationship> getDependencies() {
        return dependencies;
    }
}
