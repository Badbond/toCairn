package me.soels.thesis.analysis.statik;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import me.soels.thesis.tmp.daos.AbstractClass;
import me.soels.thesis.tmp.daos.DataClass;
import me.soels.thesis.tmp.daos.DataRelationshipType;
import me.soels.thesis.tmp.daos.OtherClass;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.thesis.analysis.statik.CustomClassOrInterfaceVisitor.VisitorResult;
import static me.soels.thesis.tmp.daos.DataRelationshipType.READ;
import static me.soels.thesis.tmp.daos.DataRelationshipType.WRITE;

/**
 * Performs static analysis on the provided project to determine the relationships between classes in the project.
 * <p>
 * We analyze method calls, constructor usage and method references on a per-class basis to determine relationships
 * between classes. For method calls and references, we optimize the search algorithm by only including those referenced
 * methods that are declared within the classes of the application. We furthermore filter out relationships to classes
 * outside of the project and to the classes themselves.
 * <p>
 * We analyze inner classes separately to have them function as separate entities instead of having method calls within
 * those influence the outer class.
 */
@Service
public class StaticRelationshipAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticRelationshipAnalysis.class);
    private final DeclaringClassResolver declaringClassResolver;
    private final CustomClassOrInterfaceVisitor classVisitor;

    public StaticRelationshipAnalysis(DeclaringClassResolver declaringClassResolver, CustomClassOrInterfaceVisitor classVisitor) {
        this.declaringClassResolver = declaringClassResolver;
        this.classVisitor = classVisitor;
    }

    public void analyze(StaticAnalysisContext context) {
        LOGGER.info("Extracting relationships");
        var start = System.currentTimeMillis();

        var visitorResults = context.getTypesAndClasses().stream()
                .map(pair -> classVisitor.visit(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());
        var methodNameSet = visitorResults.stream()
                .flatMap(visitorResult -> visitorResult.getDeclaredMethods().stream())
                .map(NodeWithSimpleName::getNameAsString)
                .collect(Collectors.toSet());

        visitorResults.forEach(visitorResult ->
                this.storeClassDependencies(context, visitorResult, methodNameSet));

        printResults(context, visitorResults, methodNameSet);
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static method call analysis took {} (H:m:s.millis)", duration);
    }

    private void storeClassDependencies(StaticAnalysisContext context,
                                        VisitorResult visitorResult,
                                        Set<String> allMethodNames) {
        var allClasses = context.getResultBuilder().getAllClasses();
        var relevantNodes = new HashMap<AbstractClass, List<Expression>>();
        relevantNodes.putAll(getRelevantMethodCalls(context, visitorResult, allMethodNames, allClasses));
        relevantNodes.putAll(getRelevantMethodReferences(context, visitorResult, allMethodNames, allClasses));
        relevantNodes.putAll(getRelevantConstructorCalls(context, visitorResult, allClasses));

        relevantNodes.forEach((key, value) -> storeRelationship(visitorResult.getCaller(), key, value, context));
    }

    private Map<AbstractClass, List<Expression>> getRelevantConstructorCalls(StaticAnalysisContext context, VisitorResult visitorResult, List<AbstractClass> allClasses) {
        return visitorResult.getObjectCreationExpressions().stream()
                .flatMap(node -> declaringClassResolver.resolveConstructorCall(context, node, allClasses).stream())
                // Filter out self invocation (e.g. when having a static method to instantiate self)
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .map(calleePair -> executeSideEffect(() -> context.getCounters().relevantConstructorCalls++, calleePair))
                .collect(groupingBy(Pair::getKey, Collectors.mapping(pair -> (Expression) pair.getValue(), Collectors.toList())));
    }

    private Map<AbstractClass, List<Expression>> getRelevantMethodReferences(StaticAnalysisContext context, VisitorResult visitorResult, Set<String> allMethodNames, List<AbstractClass> allClasses) {
        return visitorResult.getMethodReferences().stream()
                // Filter out method names that are definitely not within the application
                .filter(methodRef -> allMethodNames.contains(methodRef.getIdentifier()))
                .map(method -> executeSideEffect(() -> context.getCounters().matchingMethodReferences++, method))
                // Try to resolve the method reference to get the pair of callee and its method invoked.
                .flatMap(methodRef -> declaringClassResolver.resolveMethodReference(context, methodRef, allClasses).stream())
                // Filter out self invocation
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .map(calleePair -> executeSideEffect(() -> context.getCounters().relevantMethodReferences++, calleePair))
                // Group by callee class based on FQN with value the AST nodes relevant
                .collect(groupingBy(Pair::getKey, Collectors.mapping(pair -> (Expression) pair.getValue(), Collectors.toList())));
    }

    private Map<AbstractClass, List<Expression>> getRelevantMethodCalls(StaticAnalysisContext context, VisitorResult visitorResult, Set<String> allMethodNames, List<AbstractClass> allClasses) {
        // We need to iterate this way to resolve statements in a preorder for multiple types of nodes
        return visitorResult.getMethodCalls().stream()
                // Filter out method names that are definitely not within the application
                .filter(method -> allMethodNames.contains(method.getNameAsString()))
                .map(method -> executeSideEffect(() -> context.getCounters().matchingMethodCalls++, method))
                // Try to resolve the method call to get the pair of callee and its method invoked.
                .flatMap(method -> declaringClassResolver.resolveMethodCall(context, method, allClasses).stream())
                // Filter out self invocation
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .map(calleePair -> executeSideEffect(() -> context.getCounters().relevantMethodCalls++, calleePair))
                // Group by callee class based on FQN with value the AST nodes relevant
                .collect(groupingBy(Pair::getKey, Collectors.mapping(pair -> (Expression) pair.getValue(), Collectors.toList())));
    }

    /**
     * Identifies whether the given {@link AbstractClass} {@code caller} has a data relationship or a 'normal'
     * dependence relationship.
     * <p>
     * We mark the relationship as a data-relationship when the caller is an {@link OtherClass} and the callee is an
     * {@link DataClass}. To keep the graph simple, we don't mark data-to-data or data-to-other relationships as a
     * data relationship.
     *
     * @param caller        the source of the relationship
     * @param callee        the target of the relationship
     * @param relevantNodes the list of AST nodes related to the relationship
     * @param context       the contet to add the relationships to
     */
    private void storeRelationship(AbstractClass caller,
                                   AbstractClass callee,
                                   List<Expression> relevantNodes,
                                   StaticAnalysisContext context) {
        if (caller instanceof OtherClass && callee instanceof DataClass) {
            var type = identifyReadWrite(relevantNodes);
            context.getResultBuilder().addDataRelationship((OtherClass) caller, (DataClass) callee, type, relevantNodes.size());
        } else {
            context.getResultBuilder().addDependency(caller, callee, relevantNodes.size());
        }
    }

    /**
     * Identifies whether the caller has a read or write dependency to the callee. If any of the AST nodes signify that
     * data is being written to the callee, we will mark this relationship as {@link DataRelationshipType#WRITE}.
     * Otherwise, we will signify it as {@link DataRelationshipType#READ}.
     * <p>
     * In case the caller instantiates the callee, we always mark the relationship as {@link DataRelationshipType#WRITE}.
     * <p>
     * Then we continue analyzing the {@link MethodCallExpr} to the callee. If any of these send arguments to the
     * callee in its call, we assume that the state of the data object is changed by what is passed as argument.
     * Otherwise, we mark the relationship as {@link DataRelationshipType#READ}. We unfortunately can't analyze the
     * return type as we have not always resolved the method call (whereas we did resolve the type) and therefore
     * requires significant more effort to determine whether this method returns a value or not. Therefore, cases such
     * as toggle-methods (void methods without parameters) to toggle state in the callee, can not be identified without
     * significant additional effort.
     *
     * @param relevantNodes the AST nodes to analyze to determine the data relationship type
     * @return whether data is being read or written
     */
    private DataRelationshipType identifyReadWrite(List<Expression> relevantNodes) {
        if (relevantNodes.stream().anyMatch(Expression::isObjectCreationExpr)) {
            return WRITE;
        }
        if (relevantNodes.stream()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .anyMatch(method -> method.getArguments().size() > 1)) {
            return WRITE;
        }

        // TODO: Actually process them.. But how? We don't know the parameters (can also be 0!) and naming seems not
        //  consistent enough.
        relevantNodes.stream()
                .filter(MethodReferenceExpr.class::isInstance)
                .map(MethodReferenceExpr.class::cast)
                .findFirst().ifPresent(node -> LOGGER.info("Node {} is interesting", node.getIdentifier()));

        return READ;
    }

    private void printResults(StaticAnalysisContext context,
                              List<VisitorResult> visitorResults,
                              Set<String> methodNameSet) {
        var counters = context.getCounters();
        // For unresolved AST nodes (method calls, object creations) we can not determine the class that was called.
        LOGGER.warn("Ignored {} AST nodes as they could not be resolved. Set property 'logging=debug' to see why they " +
                "could not be resolved.", counters.unresolvedNodes);

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
                context.getResultBuilder().getDependencies().size(),
                context.getResultBuilder().getDataRelationships().size()
        );
        LOGGER.info("Total means the amount of that type of AST node found within the project");
        LOGGER.info("Matching means that the method is in the unique set of declared method names.");
        LOGGER.info("Relevant means that the after resolving the AST node, the declaring class was part of another " +
                "class within this project.");
    }

    private <T> T executeSideEffect(Runnable runnable, T value) {
        // Usage of .peek() is not recommended as some stream implementation do not support it. Therefore we have this
        // method to execute a side effect using .map().
        runnable.run();
        return value;
    }
}
