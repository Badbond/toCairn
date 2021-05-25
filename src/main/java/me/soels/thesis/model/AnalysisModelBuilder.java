package me.soels.thesis.model;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.apache.commons.lang3.tuple.Pair;

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
    private final List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> allTypes = Collections.synchronizedList(new ArrayList<>());
    private final List<OtherClass> otherClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DataClass> dataClasses = Collections.synchronizedList(new ArrayList<>());
    private final List<DependenceRelationship> dependencies = Collections.synchronizedList(new ArrayList<>());
    private final List<DataRelationship> dataRelationships = Collections.synchronizedList(new ArrayList<>());

    public AnalysisModelBuilder withAllTypes(List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> allTypes) {
        this.dataRelationships.addAll(dataRelationships);
        return this;
    }

    public AnalysisModelBuilder withOtherClasses(List<OtherClass> otherClasses) {
        this.otherClasses.addAll(otherClasses);
        return this;
    }

    public AnalysisModelBuilder withDataClasses(List<DataClass> dataClasses) {
        this.dataClasses.addAll(dataClasses);
        return this;
    }

    public AnalysisModelBuilder withDependencies(List<DependenceRelationship> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public AnalysisModelBuilder withDataRelationships(List<DataRelationship> dataRelationships) {
        this.dataRelationships.addAll(dataRelationships);
        return this;
    }

    public synchronized AnalysisModel build() {
        return new AnalysisModel(otherClasses, dataClasses, dependencies, dataRelationships);
    }
}
