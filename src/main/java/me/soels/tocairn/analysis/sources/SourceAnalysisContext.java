package me.soels.tocairn.analysis.sources;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.EvaluationInputBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context holder for source analysis.
 * <p>
 * This context holder will hold the resulting analysis results and populates that in the {@link EvaluationInputBuilder}.
 * It furthermore contains data needed to share between stages of the source analysis, utility functions on the data
 * stored within this context, and additional data in favor of debugging such as counters.
 */
@Getter
@Setter
public class SourceAnalysisContext {
    private final Path projectLocation;
    private final SourceAnalysisInput input;
    private final EvaluationInputBuilder resultBuilder;
    private final Counters counters = new Counters();
    private final List<Pair<ClassOrInterfaceDeclaration, AbstractClass>> typesAndClasses = new ArrayList<>();
    private final Map<String, Map<Integer, Long>> sourceExecutions = new HashMap<>();
    private double averageSize;

    public SourceAnalysisContext(Path projectLocation,
                                 SourceAnalysisInput input,
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
        int relevantMethodCalls = 0;
        int relevantFieldAccesses = 0;
        int relevantStaticExpressions = 0;
        int matchingImportStatements = 0;
        int relevantImportStatements = 0;
    }
}
