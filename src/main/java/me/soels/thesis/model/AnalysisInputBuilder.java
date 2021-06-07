package me.soels.thesis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for an {@link AnalysisInput}.
 * <p>
 * This builder can be used to construct the model in a parallel manner for all analysis classes. This
 * furthermore allows for modifying the model before finalizing it when it is being run through the evolutionary
 * algorithm.
 */
public class AnalysisInputBuilder {
    private final List<OtherClass> otherClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DataClass> dataClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DependenceRelationship> dependencies = Collections.synchronizedList(new ArrayList<>());
    private final List<DataRelationship> dataRelationships = Collections.synchronizedList(new ArrayList<>());

    public AnalysisInputBuilder withOtherClasses(List<OtherClass> otherClasses) {
        this.otherClasses.addAll(otherClasses);
        return this;
    }

    public AnalysisInputBuilder withDataClasses(List<DataClass> dataClasses) {
        this.dataClasses.addAll(dataClasses);
        return this;
    }

    public AnalysisInputBuilder withDependencies(List<DependenceRelationship> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public AnalysisInputBuilder withDataRelationships(List<DataRelationship> dataRelationships) {
        this.dataRelationships.addAll(dataRelationships);
        return this;
    }

    public synchronized AnalysisInput build() {
        return new AnalysisInput(otherClasses, dataClasses, dependencies, dataRelationships);
    }
}
