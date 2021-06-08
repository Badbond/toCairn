package me.soels.thesis.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for an {@link EvaluationInput}.
 * <p>
 * This builder can be used to construct and enhance the model for all analysis classes. This furthermore allows for
 * modifying the model before finalizing it when it is being run through the evolutionary algorithm.
 */
@Getter
public class EvaluationInputBuilder {
    private final UUID evaluationId;
    private final List<AbstractClass> allClasses = new ArrayList<>();
    private final List<DependenceRelationship> dependencies = new ArrayList<>();
    private final List<DataRelationship> dataRelationships = new ArrayList<>();

    /**
     * Creates a clean new builder for the evaluation with the given {@code evaluationId}.
     *
     * @param evaluationId the evaluation to create the input for
     */
    public EvaluationInputBuilder(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }

    /**
     * Creates a builder from an already existing input.
     *
     * @param input the input data to start this builder with
     */
    public EvaluationInputBuilder(EvaluationInput input) {
        this.evaluationId = input.getEvaluationId();
        this.allClasses.addAll(input.getOtherClasses());
        this.allClasses.addAll(input.getDataClasses());
        this.dependencies.addAll(input.getDependencies());
        this.dataRelationships.addAll(input.getDataRelations());
    }

    public void addDataClass(String fqn, String humanReadableName, int size) {
        allClasses.add(new DataClass(fqn, humanReadableName, size, evaluationId));
    }

    public void addOtherClass(String fqn, String humanReadableName) {
        allClasses.add(new OtherClass(fqn, humanReadableName, evaluationId));
    }

    public void addDependency(AbstractClass callee, int frequency) {
        dependencies.add(new DependenceRelationship(callee, frequency, evaluationId));
    }

    public void addDataRelationship(AbstractClass caller, AbstractClass callee, DataRelationshipType type, int frequency) {
        dataRelationships.add(new DataRelationship(callee, type, frequency, evaluationId));
    }

    public EvaluationInput build() {
        return new EvaluationInput(evaluationId, otherClasses, dataClasses, dependencies, dataRelationships);
    }

    private
}
