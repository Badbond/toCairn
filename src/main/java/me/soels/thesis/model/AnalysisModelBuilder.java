package me.soels.thesis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for an {@link AnalysisModel}.
 * <p>
 * This builder can be used to construct the model in a parallel manner for all analysis classes. This
 * furthermore allows for modifying the model before finalizing it when it is being run through the evolutionary
 * algorithm.
 */
public class AnalysisModelBuilder {
    private final List<OtherClass> otherClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DependenceRelationship> dependencies = Collections.synchronizedList(new ArrayList<>());

    public AnalysisModelBuilder withOtherClasses(List<OtherClass> otherClasses) {
        this.otherClasses.addAll(otherClasses);
        return this;
    }

    public AnalysisModelBuilder withDependencies(List<DependenceRelationship> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public List<OtherClass> getOtherClasses() {
        return otherClasses;
    }

    public List<DependenceRelationship> getDependencies() {
        return dependencies;
    }

    public synchronized AnalysisModel build() {
        return new AnalysisModel(otherClasses, dependencies);
    }
}
