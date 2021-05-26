package me.soels.thesis.analysis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Context holder for static analysis.
 * <p>
 * This context holder will hold the resulting analysis results and populates that in the {@link AnalysisModelBuilder}.
 * It furthermore contains data needed to share between stages of the static analysis, utility functions on the data
 * stored within this context, and additional data in favor of debugging such as counters.
 */
class StaticAnalysisContext {
    private final Path projectLocation;
    private final StaticAnalysisInput input;
    private final List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> classesAndTypes = new ArrayList<>();
    private final List<DependenceRelationship> relationships = new ArrayList<>();
    private final Counters counters = new Counters();

    public StaticAnalysisContext(Path projectLocation, StaticAnalysisInput input) {
        this.projectLocation = projectLocation;
        this.input = input;
    }

    public Path getProjectLocation() {
        return projectLocation;
    }

    public StaticAnalysisInput getInput() {
        return input;
    }

    public List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> getClassesAndTypes() {
        return classesAndTypes;
    }

    public void addClassesAndTypes(List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> classesAndTypes) {
        this.classesAndTypes.addAll(classesAndTypes);
    }

    public List<DataClass> getDataClasses() {
        return getClassType(DataClass.class);
    }

    public List<OtherClass> getOtherClasses() {
        return getClassType(OtherClass.class);
    }

    public List<DependenceRelationship> getRelationships() {
        return relationships;
    }

    public void addRelationships(List<DependenceRelationship> relationships) {
        this.relationships.addAll(relationships);
    }

    public List<DataRelationship> getDataRelationships() {
        return relationships.stream()
                .filter(relationship -> DataRelationship.class.isAssignableFrom(relationship.getClass()))
                .map(DataRelationship.class::cast)
                .collect(Collectors.toList());
    }

    public Counters getCounters() {
        return counters;
    }

    /**
     * Applies the information held in this context to the {@link AnalysisModelBuilder}.
     *
     * @param builder the builder to apply the context information to
     */
    public void applyResults(AnalysisModelBuilder builder) {
        builder.withOtherClasses(getClassType(OtherClass.class))
                .withDataClasses(getClassType(DataClass.class))
                .withDependencies(relationships)
                .withDataRelationships(getDataRelationships());
    }

    private <T extends AbstractClass> List<T> getClassType(Class<T> wantedClass) {
        return classesAndTypes.stream()
                .map(Pair::getKey)
                .filter(clazz -> wantedClass.isAssignableFrom(clazz.getClass()))
                .map(wantedClass::cast)
                .collect(Collectors.toList());
    }

    static class Counters {
        int unresolvedNodes = 0;
        int relevantConstructorCalls = 0;
        int matchingMethodReferences = 0;
        int relevantMethodReferences = 0;
        int matchingMethodCalls = 0;
        int relevantMethodCalls;
    }
}
