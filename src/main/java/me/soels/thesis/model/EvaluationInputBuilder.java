package me.soels.thesis.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for an {@link EvaluationInput}.
 * <p>
 * This builder can be used to construct and enhance the model for all analysis classes. This furthermore allows for
 * modifying the model before finalizing it when it is being run through the evolutionary algorithm. Lastly, it can be
 * used to construct an {@link EvaluationInput} from providing the stored data.
 */
@Getter
public class EvaluationInputBuilder {
    private final List<AbstractClass> classes = new ArrayList<>();
    private final List<DependenceRelationship> dependencies = new ArrayList<>();
    private final List<DataRelationship> dataRelationships = new ArrayList<>();

    /**
     * Sets the {@link #getClasses()} of this builder.
     *
     * @param classes the classes to set
     * @return this builder
     */
    public EvaluationInputBuilder withClasses(List<? extends AbstractClass> classes) {
        this.classes.addAll(classes);
        return this;
    }

    /**
     * Sets the {@link #getDependencies()} of this builder.
     * <p>
     * Note, this should include all dependencies, also child dependencies such as {@link DataRelationship}.
     *
     * @param dependencies the dependencies to set
     * @return this builder
     */
    public EvaluationInputBuilder withDependencies(List<DependenceRelationship> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    /**
     * Sets the {@link #getDataRelationships()}} of this builder.
     * <p>
     * Note, that {@link #withDependencies(List)} does not set the {@code dataRelationships} and therefore you need to
     * invoke this endpoint separately.
     *
     * @param dataRelationships the data relationships to set
     * @return this builder
     */
    public EvaluationInputBuilder withDataRelationships(List<DataRelationship> dataRelationships) {
        this.dataRelationships.addAll(dataRelationships);
        return this;
    }

    /**
     * Adds a {@link DataClass} to the persisted state of this builder.
     *
     * @param fqn               the fully qualified name of the class
     * @param humanReadableName the human readable name of the class
     * @param size              the size of the data class
     * @return this builder
     */
    public DataClass addDataClass(String fqn, String humanReadableName, int size) {
        var dataClass = new DataClass(fqn, humanReadableName, size);
        classes.add(dataClass);
        return dataClass;
    }

    /**
     * Adds an {@link OtherClass} to the persisted state of this builder.
     *
     * @param fqn               the fully qualified name of the class
     * @param humanReadableName the human readable name of the class
     * @return this builder
     */
    public OtherClass addOtherClass(String fqn, String humanReadableName) {
        var otherClass = new OtherClass(fqn, humanReadableName);
        classes.add(otherClass);
        return otherClass;
    }

    /**
     * Adds a {@link DependenceRelationship} to the persisted state of this builder.
     * <p>
     * This furthermore adds the constructed relationship on the caller object as well such that it will be persisted
     * once saved to the repository
     *
     * @param caller    the class that calls the callee
     * @param callee    the class that is being called by the caller
     * @param frequency the frequency in which these two classes depend on each other
     */
    public void addDependency(AbstractClass caller, AbstractClass callee, int frequency) {
        var dependency = new DependenceRelationship(callee, frequency);
        caller.getDependenceRelationships().add(dependency);
        dependencies.add(dependency);
    }

    /**
     * Adds a {@link DataRelationship} to the persisted state of this builder.
     * <p>
     * Note that this will also add this relationship to {@link #getDependencies()} as a {@link DataRelationship} is an
     * {@link DependenceRelationship}.
     * <p>
     * This furthermore adds the constructed relationship on the caller object as well such that it will be persisted
     * once saved to the repository
     *
     * @param caller    the class that calls the callee
     * @param callee    the class that is being called by the caller
     * @param frequency the frequency in which these two classes depend on each other
     */
    public void addDataRelationship(OtherClass caller, DataClass callee, DataRelationshipType type, int frequency) {
        var dataRelationship = new DataRelationship(callee, type, frequency);
        caller.getDataRelationships().add(dataRelationship);
        dataRelationships.add(dataRelationship);
        dependencies.add(dataRelationship);
    }

    /**
     * Returns the data classes stored in the builder.
     *
     * @return the data classes stored in the builder
     */
    public List<DataClass> getDataClasses() {
        return getTypesOfClasses(DataClass.class);
    }

    /**
     * Returns the other classes stored in the builder.
     *
     * @return the other classes stored in the builder
     */
    public List<OtherClass> getOtherClasses() {
        return getTypesOfClasses(OtherClass.class);
    }

    /**
     * Builds the {@link EvaluationInput} from the values stored in this builder's state.
     *
     * @return the input graph
     */
    public EvaluationInput build() {
        return new EvaluationInput(getOtherClasses(), getDataClasses(), dependencies, dataRelationships);
    }

    private <T extends AbstractClass> List<T> getTypesOfClasses(Class<T> expectedClass) {
        return classes.stream()
                .filter(clazz -> expectedClass.isAssignableFrom(clazz.getClass()))
                .map(expectedClass::cast)
                .collect(Collectors.toList());
    }
}
