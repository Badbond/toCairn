package me.soels.thesis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for an {@link EvaluationInput}.
 * <p>
 * This builder can be used to construct the model in a parallel manner for all analysis classes. This
 * furthermore allows for modifying the model before finalizing it when it is being run through the evolutionary
 * algorithm.
 */
public class EvaluationInputBuilder {
    private final List<OtherClass> otherClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DataClass> dataClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DependenceRelationship> dependencies = Collections.synchronizedList(new ArrayList<>());
    private final List<DataRelationship> dataRelationships = Collections.synchronizedList(new ArrayList<>());

    public EvaluationInputBuilder withOtherClasses(List<OtherClass> otherClasses) {
        this.otherClasses.addAll(otherClasses);
        return this;
    }

    public EvaluationInputBuilder withDataClasses(List<DataClass> dataClasses) {
        this.dataClasses.addAll(dataClasses);
        return this;
    }

    public EvaluationInputBuilder withDependencies(List<DependenceRelationship> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public EvaluationInputBuilder withDataRelationships(List<DataRelationship> dataRelationships) {
        this.dataRelationships.addAll(dataRelationships);
        return this;
    }

    public synchronized EvaluationInput build() {
        return new EvaluationInput(otherClasses, dataClasses, dependencies, dataRelationships);
    }
}
