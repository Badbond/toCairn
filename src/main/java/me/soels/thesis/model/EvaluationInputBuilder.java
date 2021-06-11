package me.soels.thesis.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for an {@link EvaluationInput}.
 * <p>
 * This builder can be used to construct and enhance the model for all analysis classes. This furthermore allows for
 * modifying the model before finalizing it when it is being run through the evolutionary algorithm.
 */
@Getter
public class EvaluationInputBuilder {
    private final List<AbstractClass> allClasses = new ArrayList<>();
    private final List<DependenceRelationship> dependencies = new ArrayList<>();
    private final List<DataRelationship> dataRelationships = new ArrayList<>();

    /**
     * Creates a clean new builder to construct an input graph with.
     */
    public EvaluationInputBuilder() {
    }

    /**
     * Creates a builder from an already existing input.
     *
     * @param input the input data to start this builder with
     */
    public EvaluationInputBuilder(EvaluationInput input) {
        this.allClasses.addAll(input.getOtherClasses());
        this.allClasses.addAll(input.getDataClasses());
        this.dependencies.addAll(input.getDependencies());
        this.dataRelationships.addAll(input.getDataRelations());
    }

    public EvaluationInputBuilder withClasses(List<? extends AbstractClass> classes) {
        this.allClasses.addAll(classes);
        return this;
    }

    public EvaluationInputBuilder withDependencies(List<DependenceRelationship> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public DataClass addDataClass(String fqn, String humanReadableName, int size) {
        var dataClass = new DataClass(fqn, humanReadableName, size);
        allClasses.add(dataClass);
        return dataClass;
    }

    public OtherClass addOtherClass(String fqn, String humanReadableName) {
        var otherClass = new OtherClass(fqn, humanReadableName);
        allClasses.add(otherClass);
        return otherClass;
    }

    public DependenceRelationship addDependency(AbstractClass caller, AbstractClass callee, int frequency) {
        var dependency = new DependenceRelationship(callee, frequency);
        caller.getDependenceRelationships().add(dependency);
        dependencies.add(dependency);
        return dependency;
    }

    public DataRelationship addDataRelationship(OtherClass caller, DataClass callee, DataRelationshipType type, int frequency) {
        var dataRelationship = new DataRelationship(callee, type, frequency);
        caller.getDataRelationships().add(dataRelationship);
        dataRelationships.add(dataRelationship);
        return dataRelationship;
    }

    public List<DataClass> getDataClasses() {
        return getTypesOfClasses(DataClass.class);
    }

    public List<OtherClass> getOtherClasses() {
        return getTypesOfClasses(OtherClass.class);
    }

    public EvaluationInput build() {
        return new EvaluationInput(getOtherClasses(), getDataClasses(), dependencies, dataRelationships);
    }

    private <T extends AbstractClass> List<T> getTypesOfClasses(Class<T> expectedClass) {
        return allClasses.stream()
                .filter(clazz -> expectedClass.isAssignableFrom(clazz.getClass()))
                .map(expectedClass::cast)
                .collect(Collectors.toList());
    }
}
