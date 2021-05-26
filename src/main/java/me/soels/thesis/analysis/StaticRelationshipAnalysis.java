package me.soels.thesis.analysis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.thesis.model.DataRelationshipType.READ;
import static me.soels.thesis.model.DataRelationshipType.WRITE;

/**
 * Performs static analysis on the provided project to determine the relationships between classes in the project.
 */
public class StaticRelationshipAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticRelationshipAnalysis.class);
    private final MethodCallDeclaringClassResolver declaringClassResolver = new MethodCallDeclaringClassResolver();
    private final CustomClassOrInterfaceVisitor classVisitor = new CustomClassOrInterfaceVisitor();
    private int relevantCount = 0;

    public void analyze(StaticAnalysisContext context) {
        LOGGER.info("Extracting relationships");
        var start = System.currentTimeMillis();
        var allTypes = context.getClassesAndTypes();
        var allMethodNames = allTypes.stream()
                .flatMap(pair -> pair.getValue().findAll(MethodDeclaration.class).stream())
                .map(NodeWithSimpleName::getNameAsString)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        var dependenceRelationships = allTypes.stream()
                .flatMap(pair -> this.resolveClassDependencies(context, pair.getKey(), allMethodNames, pair.getValue()).stream())
                .collect(Collectors.toList());
        context.addRelationships(dependenceRelationships);

        // TODO: Clean up the way we count things here.
        LOGGER.info("Graph edges results:" +
                        "\n\tTotal unique method names:     {}" +
                        "\n\tTotal matching method calls:   {}" +
                        "\n\tUnresolved calls:              {}" +
                        "\n\tResolved calls:                {}" +
                        "\n\tTo classes within application: {}" +
                        "\n\tExcluding self-reference:      {}" +
                        "\n\tDependence relationships:      {}" +
                        "\n\tOf which data relationships:   {}",
                allMethodNames.size(), declaringClassResolver.getTotalCount(), declaringClassResolver.getErrorCount(),
                declaringClassResolver.getIdentifiedCount(), declaringClassResolver.getCalleeCount(),
                relevantCount, dependenceRelationships.size(), context.getDataRelationships().size());

        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static method call analysis took {} (H:m:s.millis)", duration);
    }


    private List<DependenceRelationship> resolveClassDependencies(StaticAnalysisContext context,
                                                                  AbstractClass caller,
                                                                  List<String> allMethodNames,
                                                                  ClassOrInterfaceDeclaration classDeclaration) {
        var allClasses = context.getClassesAndTypes().stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        var result = classVisitor.visit(classDeclaration);

        // We need to iterate this way to resolve statements in a preorder for multiple types of nodes
        return result.getMethodCalls().stream()
                // Filter out method names that are definitely not within the application
                .filter(method -> allMethodNames.contains(method.getNameAsString()))
                // Try to resolve the method call to get the pair of callee and its method invoked.
                .flatMap(method -> declaringClassResolver.getDeclaringClass(method, allClasses).stream())
                // Filter out self invocation
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(caller.getIdentifier()))
                // TODO: Should we filter out hashcode(), equals() and toString() usage?
                // Group by callee class based on FQN
                .collect(groupingBy(Pair::getKey))
                .values().stream()
                // For every callee, identify the relationship
                .map(calleePairs -> identifyRelationship(caller, calleePairs))
                .collect(Collectors.toList());
    }

    private DependenceRelationship identifyRelationship(AbstractClass clazz, List<Pair<AbstractClass, MethodCallExpr>> calleeMethods) {
        relevantCount += calleeMethods.size();
        var target = calleeMethods.get(0).getKey();
        var methods = calleeMethods.stream().map(Pair::getValue).collect(Collectors.toList());
        if (clazz instanceof OtherClass && target instanceof DataClass) {
            return new DataRelationship((OtherClass) clazz, (DataClass) target, identifyReadWrite(methods), calleeMethods.size());
        } else {
            return new DependenceRelationship(clazz, target, calleeMethods.size());
        }
    }

    /**
     * Identifies whether the caller has a read or write dependency to the callee. If any of the calls signify that
     * data is being written to the callee, we will mark this relationship as {@link DataRelationshipType#WRITE}.
     * Otherwise, we will signify it as {@link DataRelationshipType#READ}.
     * <p>
     * We unfortunately can't analyze the return type as we have not always resolved the method call (whereas we
     * did resolve the type) and therefore requires significant more effort to determine whether this method returns a
     * value or not. Because in that case you could catch toggle-like methods that do not provide arguments but still
     * change the state of the data.
     *
     * @param methodsCalled the multiple method calls to analyze
     * @return whether data is being read or written
     */
    private DataRelationshipType identifyReadWrite(List<MethodCallExpr> methodsCalled) {
        // When our caller has to provide arguments to the callee, we assume it is modifying the state of the callee
        // with those values
        return methodsCalled.stream().anyMatch(method -> method.getArguments().size() > 1) ? WRITE : READ;
    }
}
