package me.soels.thesis.analysis;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.thesis.analysis.CustomClassOrInterfaceVisitor.VisitorResult;
import static me.soels.thesis.model.DataRelationshipType.READ;
import static me.soels.thesis.model.DataRelationshipType.WRITE;

/**
 * Performs static analysis on the provided project to determine the relationships between classes in the project.
 */
public class StaticRelationshipAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticRelationshipAnalysis.class);
    private final MethodCallDeclaringClassResolver declaringClassResolver = new MethodCallDeclaringClassResolver();
    private final CustomClassOrInterfaceVisitor classVisitor = new CustomClassOrInterfaceVisitor();

    public void analyze(StaticAnalysisContext context) {
        LOGGER.info("Extracting relationships");
        var start = System.currentTimeMillis();

        var visitorResults = context.getClassesAndTypes().stream()
                .map(pair -> classVisitor.visit(pair.getValue(), pair.getKey()))
                .collect(Collectors.toList());
        var methodNameSet = visitorResults.stream()
                .flatMap(visitorResult -> visitorResult.getDeclaredMethods().stream())
                .map(NodeWithSimpleName::getNameAsString)
                .collect(Collectors.toSet());

        var relationships = visitorResults.stream()
                .flatMap(visitorResult -> this.resolveClassDependencies(context, visitorResult, methodNameSet).stream())
                .collect(Collectors.toList());
        context.addRelationships(relationships);

        printResults(context, visitorResults, methodNameSet);
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static method call analysis took {} (H:m:s.millis)", duration);
    }

    private List<DependenceRelationship> resolveClassDependencies(StaticAnalysisContext context,
                                                                  VisitorResult visitorResult,
                                                                  Set<String> allMethodNames) {
        var allClasses = context.getClassesAndTypes().stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        // TODO: Process object creation and references including counters

        // We need to iterate this way to resolve statements in a preorder for multiple types of nodes
        return visitorResult.getMethodCalls().stream()
                // Filter out method names that are definitely not within the application
                .filter(method -> allMethodNames.contains(method.getNameAsString()))
                // Try to resolve the method call to get the pair of callee and its method invoked.
                .flatMap(method -> declaringClassResolver.getDeclaringClass(context, method, allClasses).stream())
                // Filter out self invocation
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .peek(calleePair -> context.getCounters().relevantMethodCalls++)
                // Group by callee class based on FQN
                .collect(groupingBy(Pair::getKey))
                .values().stream()
                // For every callee, identify the relationship
                .map(calleePairs -> identifyRelationship(visitorResult.getCaller(), calleePairs))
                .collect(Collectors.toList());
    }

    private DependenceRelationship identifyRelationship(AbstractClass clazz,
                                                        List<Pair<AbstractClass, MethodCallExpr>> calleeMethods) {
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
        return methodsCalled.stream().anyMatch(method -> method.getArguments().size() > 1) ? WRITE : READ;
    }

    private void printResults(StaticAnalysisContext context,
                              List<VisitorResult> visitorResults,
                              Set<String> methodNameSet) {
        var counters = context.getCounters();
        // These AST nodes (method calls, object creations) could not be resolved and thus we could not determine the
        // class that was called as part of that node.
        LOGGER.warn("Ignored {} AST nodes as they could not be resolved. Set property 'logging=debug' to see why it could not be resolved.", counters.unresolvedNodes);

        LOGGER.info("Graph edges results:" +
                        "\n\tTotal constructor calls:           {}" +
                        "\n\tRelevant constructor calls         {}" +
                        "\n\tTotal unique method names:         {}" +
                        "\n\tTotal method references:           {}" +
                        "\n\tMatching method references:        {}" +
                        "\n\tRelevant method references:        {}" +
                        "\n\tTotal method calls:                {}" +
                        "\n\tMatching method calls:             {}" +
                        "\n\tRelevant method calls:             {}" + // To other classes within the project
                        "\n\tTotal dependence relationships:    {}" +
                        "\n\tOf which data relationships:       {}",
                visitorResults.stream().mapToInt(res -> res.getObjectCreationExpressions().size()).sum(),
                counters.relevantConstructorCalls,
                methodNameSet.size(),
                visitorResults.stream().mapToInt(res -> res.getMethodReferences().size()).sum(),
                counters.matchingMethodReferences,
                counters.relevantMethodReferences,
                visitorResults.stream().mapToInt(res -> res.getMethodCalls().size()).sum(),
                counters.matchingMethodCalls,
                counters.relevantMethodCalls,
                context.getRelationships().size(),
                context.getDataRelationships().size()
        );
        LOGGER.info("Total means the amount of that type of AST node found within the project");
        LOGGER.info("Matching means that the method is in the unique set of declared method names.");
        LOGGER.info("Relevant means that the after resolving the AST node, the declaring class was part of another " +
                "class within this project.");
    }
}
