package me.soels.thesis.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static me.soels.thesis.util.GenericCollectionExtractor.extractType;

/**
 * Builder for an {@link EvaluationInput}.
 * <p>
 * This builder can be used to construct and enhance the model for all analysis classes. This furthermore allows for
 * modifying the model before finalizing it when it is being run through the evolutionary algorithm. Lastly, it can be
 * used to construct an {@link EvaluationInput} from providing the stored data.
 */
@Getter
public class EvaluationInputBuilder {
    private final List<AbstractClass> classes;

    public EvaluationInputBuilder(List<? extends AbstractClass> classes) {
        // Construct an array list such that it will be mutable.
        this.classes = new ArrayList<>(classes);
    }

    /**
     * Adds a {@link DataClass} to the persisted state of this builder.
     *
     * @param fqn               the fully qualified name of the class
     * @param humanReadableName the human readable name of the class
     * @param location          the location of the source file for this class
     * @return this builder
     */
    public DataClass addDataClass(String fqn, String humanReadableName, String location) {
        var dataClass = new DataClass(fqn, humanReadableName, location);
        classes.add(dataClass);
        return dataClass;
    }

    /**
     * Adds an {@link OtherClass} to the persisted state of this builder.
     *
     * @param fqn               the fully qualified name of the class
     * @param humanReadableName the human readable name of the class
     * @param location          the location of the source file for this class
     * @return this builder
     */
    public OtherClass addOtherClass(String fqn, String humanReadableName, String location) {
        var otherClass = new OtherClass(fqn, humanReadableName, location);
        classes.add(otherClass);
        return otherClass;
    }

    /**
     * Creates a {@link DependenceRelationship} between the given {@code caller} and {@code callee}.
     *
     * @param caller      the class that calls the callee
     * @param callee      the class that is being called by the caller
     * @param staticFreq  the number of places these two classes depend on each other
     * @param dynamicFreq the dynamic frequency in which these two classes depend on each other
     */
    public void addDependency(AbstractClass caller, AbstractClass callee, int staticFreq, Long dynamicFreq) {
        var dependency = new DependenceRelationship(callee, staticFreq, dynamicFreq);
        caller.getDependenceRelationships().add(dependency);
    }

    /**
     * Creates a {@link DataRelationship} between the given {@code caller} and {@code callee}.
     * <p>
     * This furthermore adds the constructed relationship on the caller object as well such that it will be persisted
     * once saved to the repository
     *
     * @param caller          the class that calls the callee
     * @param callee          the class that is being called by the caller
     * @param staticFrequency the number of places these two classes depend on each other
     * @param dynamicFreq     the dynamic frequency in which these two classes depend on each other
     */
    public void addDataRelationship(OtherClass caller, DataClass callee, DataRelationshipType type, int staticFrequency, Long dynamicFreq) {
        var dataRelationship = new DataRelationship(callee, type, staticFrequency, dynamicFreq);
        caller.getDataRelationships().add(dataRelationship);
    }

    /**
     * Returns the data classes stored in the builder.
     *
     * @return the data classes stored in the builder
     */
    public List<DataClass> getDataClasses() {
        return extractType(classes, DataClass.class);
    }

    /**
     * Returns the other classes stored in the builder.
     *
     * @return the other classes stored in the builder
     */
    public List<OtherClass> getOtherClasses() {
        return extractType(classes, OtherClass.class);
    }

    /**
     * Builds the {@link EvaluationInput} from the values stored in this builder's state.
     *
     * @return the input graph
     */
    public EvaluationInput build() {
        return new EvaluationInput(classes);
    }
}
