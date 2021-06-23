package me.soels.thesis.analysis.statik;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Getter;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.EvaluationInputBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Context holder for static analysis.
 * <p>
 * This context holder will hold the resulting analysis results and populates that in the {@link EvaluationInputBuilder}.
 * It furthermore contains data needed to share between stages of the static analysis, utility functions on the data
 * stored within this context, and additional data in favor of debugging such as counters.
 */
@Getter
public class StaticAnalysisContext {
    private final Path projectLocation;
    private final StaticAnalysisInput input;
    private final EvaluationInputBuilder resultBuilder;
    private final Counters counters = new Counters();
    private final List<Pair<ClassOrInterfaceDeclaration, AbstractClass>> typesAndClasses = new ArrayList<>();

    public StaticAnalysisContext(Path projectLocation,
                                 StaticAnalysisInput input,
                                 EvaluationInputBuilder resultBuilder) {
        this.projectLocation = projectLocation;
        this.input = input;
        this.resultBuilder = resultBuilder;
    }

    /**
     * Sets the types and classes identified during class analysis to use in relationship analysis as well.
     *
     * @param typesAndClasses the types and classes to persist in context
     */
    public void setTypesAndClasses(List<Pair<ClassOrInterfaceDeclaration, AbstractClass>> typesAndClasses) {
        this.typesAndClasses.addAll(typesAndClasses);
    }

    static class Counters {
        int unresolvedNodes = 0;
        int matchingConstructorCalls = 0;
        int relevantConstructorCalls = 0;
        int matchingMethodReferences = 0;
        int relevantMethodReferences = 0;
        int matchingMethodCalls = 0;
        int relevantMethodCalls;
    }
}
