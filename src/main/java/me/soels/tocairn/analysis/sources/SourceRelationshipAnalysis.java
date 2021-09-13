package me.soels.tocairn.analysis.sources;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import me.soels.tocairn.analysis.sources.CustomClassOrInterfaceVisitor.VisitorResult;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.DataClass;
import me.soels.tocairn.model.DataRelationshipType;
import me.soels.tocairn.model.OtherClass;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.tocairn.model.DataRelationshipType.READ;
import static me.soels.tocairn.model.DataRelationshipType.WRITE;
import static me.soels.tocairn.util.Constants.PRIMITIVE_STRING;
import static org.hamcrest.Matchers.*;

/**
 * Performs source analysis on the provided project to determine the relationships between classes in the project.
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
public class SourceRelationshipAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceRelationshipAnalysis.class);
    private final DeclaringClassResolver declaringClassResolver;
    private final CustomClassOrInterfaceVisitor classVisitor;

    public SourceRelationshipAnalysis(DeclaringClassResolver declaringClassResolver, CustomClassOrInterfaceVisitor classVisitor) {
        this.declaringClassResolver = declaringClassResolver;
        this.classVisitor = classVisitor;
    }

    public void analyze(SourceAnalysisContext context) {
        LOGGER.info("Extracting relationships");
        var start = System.currentTimeMillis();

        context.setAverageSize(context.getResultBuilder().getClasses().stream()
                .mapToLong(AbstractClass::getSize)
                .average()
                .orElse(1L));

        var visitorResults = context.getTypesAndClasses().stream()
                .map(pair -> classVisitor.visit(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());
        var methodNameSet = visitorResults.stream()
                .flatMap(visitorResult -> visitorResult.getDeclaredMethods().stream())
                .map(NodeWithSimpleName::getNameAsString)
                .collect(Collectors.toSet());
        var classNameSet = context.getTypesAndClasses().stream()
                .map(pair -> pair.getKey().getNameAsString())
                .collect(Collectors.toSet());

        for (int i = 0; i < visitorResults.size(); i++) {
            if (i != 0 && i % 100 == 0) {
                LOGGER.info("... Processed relationships for {} classes", i);
            }
            this.storeClassDependencies(context, visitorResults.get(i), methodNameSet, classNameSet);
        }

        printResults(context, visitorResults, methodNameSet);
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static method call analysis took {} (H:m:s.millis)", duration);
    }

    private void storeClassDependencies(SourceAnalysisContext context,
                                        VisitorResult visitorResult,
                                        Set<String> allMethodNames,
                                        Set<String> classNameSet) {
        var allClasses = context.getResultBuilder().getClasses();
        var relevantNodes = new HashMap<AbstractClass, List<Expression>>();
        relevantNodes.putAll(getRelevantMethodCalls(context, visitorResult, allMethodNames, allClasses));
        relevantNodes.putAll(getRelevantMethodReferences(context, visitorResult, allMethodNames, allClasses));
        relevantNodes.putAll(getRelevantConstructorCalls(context, visitorResult, allClasses, classNameSet));
        relevantNodes.putAll(getRelevantFieldAccess(context, visitorResult, allClasses));
        relevantNodes.putAll(getRelevantStaticImportUsage(context, visitorResult, allClasses));
        relevantNodes.forEach((key, value) -> storeRelationship(visitorResult.getCaller(), key, value, context));

        getRelevantRemainingImportDecl(context, visitorResult, relevantNodes, allClasses)
                .forEach(callee -> context.getResultBuilder().addDependency(visitorResult.getCaller(), callee, 1, 0L, Collections.emptyMap()));
    }

    private Map<AbstractClass, List<Expression>> getRelevantConstructorCalls(SourceAnalysisContext context,
                                                                             VisitorResult visitorResult,
                                                                             List<AbstractClass> allClasses,
                                                                             Set<String> classNameSet) {
        return visitorResult.getObjectCreationExpressions().stream()
                // Filter out only constructor calls to classes we have visited
                .filter(expression -> classNameSet.contains(expression.getType().getNameAsString()))
                .map(method -> executeSideEffect(() -> context.getCounters().matchingConstructorCalls++, method))
                // Try to resolve the object creation expression to get the pair of callee and its method invoked.
                .flatMap(node -> declaringClassResolver.resolveConstructorCall(context, node, allClasses).stream())
                // Filter out self invocation (e.g. when having a static method to instantiate self)
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .map(calleePair -> executeSideEffect(() -> context.getCounters().relevantConstructorCalls++, calleePair))
                .collect(groupingBy(Pair::getKey, Collectors.mapping(pair -> (Expression) pair.getValue(), Collectors.toList())));
    }

    private Map<AbstractClass, List<Expression>> getRelevantMethodReferences(SourceAnalysisContext context,
                                                                             VisitorResult visitorResult,
                                                                             Set<String> allMethodNames,
                                                                             List<AbstractClass> allClasses) {
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

    private Map<AbstractClass, List<Expression>> getRelevantMethodCalls(SourceAnalysisContext context,
                                                                        VisitorResult visitorResult,
                                                                        Set<String> allMethodNames,
                                                                        List<AbstractClass> allClasses) {
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

    private Map<AbstractClass, List<Expression>> getRelevantFieldAccess(SourceAnalysisContext context,
                                                                        VisitorResult visitorResult,
                                                                        List<AbstractClass> allClasses) {
        // We need to iterate this way to resolve statements in a preorder for multiple types of nodes
        return visitorResult.getFieldAccesses().stream()
                // Filter out field access for this and super
                .filter(fieldAccessExpr -> !(fieldAccessExpr.getScope() instanceof ThisExpr) &&
                        !(fieldAccessExpr.getScope() instanceof SuperExpr))
                // Try to resolve the field access to get the pair of callee and its field accessed.
                .flatMap(fieldAccess -> declaringClassResolver.resolveFieldAccess(context, fieldAccess, allClasses).stream())
                // Filter out self invocation
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .map(calleePair -> executeSideEffect(() -> context.getCounters().relevantFieldAccesses++, calleePair))
                // Group by callee class based on FQN with value the AST nodes relevant
                .collect(groupingBy(Pair::getKey, Collectors.mapping(pair -> (Expression) pair.getValue(), Collectors.toList())));
    }

    private Map<AbstractClass, List<Expression>> getRelevantStaticImportUsage(SourceAnalysisContext context,
                                                                              VisitorResult visitorResult,
                                                                              List<AbstractClass> allClasses) {
        // The CustomClassOrInterfaceVisitor already filtered out NameExpr that are not based on static imports
        return visitorResult.getStaticNameExpressions().stream()
                // Try to resolve the name creation expression to get the pair of callee and its static field accessed.
                .flatMap(node -> declaringClassResolver.resolveNameExpr(context, node, allClasses).stream())
                // Filter out self invocation (e.g. when having a static method to instantiate self)
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(visitorResult.getCaller().getIdentifier()))
                .map(calleePair -> executeSideEffect(() -> context.getCounters().relevantStaticExpressions++, calleePair))
                .collect(groupingBy(Pair::getKey, Collectors.mapping(pair -> (Expression) pair.getValue(), Collectors.toList())));
    }

    private List<AbstractClass> getRelevantRemainingImportDecl(SourceAnalysisContext context,
                                                               VisitorResult visitorResult,
                                                               HashMap<AbstractClass, List<Expression>> relevantNodes,
                                                               List<AbstractClass> allClasses) {
        // The CustomClassOrInterfaceVisitor already filtered out NameExpr that are not based on static imports
        return visitorResult.getRegularImports().stream()
                // Include only import statements to classes in analysis.
                .filter(importDecl -> allClasses.stream().anyMatch(clazz -> clazz.getIdentifier().equals(importDecl.getNameAsString())))
                .map(importDecl -> executeSideEffect(() -> context.getCounters().matchingImportStatements++, importDecl))
                // Filter out only import statements for classes which we have no relationship yet
                .filter(importDecl -> relevantNodes.keySet().stream()
                        .map(AbstractClass::getIdentifier)
                        .noneMatch(fqn -> importDecl.getNameAsString().equals(fqn)))
                // Map the import statement to the class
                .flatMap(importDecl -> allClasses.stream()
                        .filter(clazz -> clazz.getIdentifier().equals(importDecl.getNameAsString())))
                .distinct()
                // Try to resolve the name creation expression to get the pair of callee and its static field accessed.
                .map(callee -> executeSideEffect(() -> context.getCounters().relevantImportStatements++, callee))
                .collect(Collectors.toList());
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
     * @param context       the context to add the relationships to
     */
    private void storeRelationship(AbstractClass caller,
                                   AbstractClass callee,
                                   List<Expression> relevantNodes,
                                   SourceAnalysisContext context) {
        var dynamicFreq = getDynamicFreq(caller, relevantNodes, context);
        var dynamicFreqSum = dynamicFreq.values().stream()
                .mapToLong(value -> value)
                .sum();

        var sharedClasses = dynamicFreq.entrySet().stream()
                .flatMap(entry -> getSharedClasses(entry.getKey(), entry.getValue()).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum));

        if (caller instanceof OtherClass && callee instanceof DataClass) {
            var type = identifyReadWrite(relevantNodes);
            context.getResultBuilder().addDataRelationship((OtherClass) caller,
                    (DataClass) callee,
                    type,
                    relevantNodes.size(),
                    dynamicFreqSum,
                    sharedClasses
            );
        } else {
            context.getResultBuilder().addDependency(caller, callee, relevantNodes.size(), dynamicFreqSum, sharedClasses);
        }
    }

    /**
     * Gets the classes shared as part of the dependencies in these two classes.
     *
     * @param expr        the dependency exprepssion
     * @param dynamicFreq how often the expression has been executed dynamically
     * @return which classes have been shared in the expressions and how often
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // Safe as type is the maximum common type that it can be.
    private Map<String, Long> getSharedClasses(Expression expr, Long dynamicFreq) {
        if (!(expr instanceof NodeWithTypeArguments)) {
            LOGGER.info("Can not determine how much data is shared for expression {}", expr);
            return Collections.emptyMap();
        }

        Optional<NodeList<Type>> typeArguments = ((NodeWithTypeArguments) expr).getTypeArguments();
        if (typeArguments.isEmpty()) {
            // No arguments shared
            return Collections.emptyMap();
        }

        return typeArguments.get().stream()
                .map(type -> getFqnFromType(type, expr).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(fqn -> fqn, fqn -> dynamicFreq));
    }

    private Optional<String> getFqnFromType(Type type, Expression expr) {
        try {
            return type.isPrimitiveType() ? Optional.of(PRIMITIVE_STRING) :
                    type.resolve().asReferenceType().getTypeDeclaration()
                            .map(ResolvedTypeDeclaration::getQualifiedName);
        } catch (Exception e) {
            LOGGER.debug("Could not determine type of argument {} in expression {}", type, expr, e);
            return Optional.empty();
        }
    }

    /**
     * Retrieves the frequency from dynamic analysis based on the source file stored in the JaCoCo report matching
     * {@code caller}.
     * <p>
     * The dynamic analysis will be determined from the execution counts on the lines matching the given expressions
     * {@code relevantNodes}. If an expression spans multiple lines, we will take the {@code max} of those lines.
     * All frequencies measured on these expressions are summed to determine the dynamic frequency.
     *
     * @param caller        the caller to retrieve dynamic frequency for
     * @param relevantNodes the expressions that we need to retrieve dynamic frequency for
     * @param context       the context containing the JaCoCo execution counts
     * @return the dynamic frequency or {@code null} if the source file was not found
     */
    private Map<Expression, Long> getDynamicFreq(AbstractClass
                                                         caller, List<Expression> relevantNodes, SourceAnalysisContext context) {
        var source = context.getSourceExecutions().entrySet().stream()
                .filter(entry -> caller.getIdentifier().contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
        if (source.isEmpty()) {
            // We do not have dynamic data present for this caller.
            if (!context.getSourceExecutions().isEmpty()) {
                // We did do some dynamic analysis, so we should better warn our users
                LOGGER.warn("Could not find source file for {} in JaCoCo report", caller.getIdentifier());
            }
            LOGGER.debug("Not applying dynamic frequency from {} as its source file in dynamic analysis was not found", caller.getIdentifier());
            return Collections.emptyMap();
        }

        var callerExecutionCounts = source.get();
        return relevantNodes.stream()
                .filter(node -> node.getRange().isPresent())
                .map(node -> Triple.of(node, node.getRange().get().begin.line, node.getRange().get().end.line))
                .map(triple -> Pair.of(triple.getLeft(),
                        callerExecutionCounts.entrySet().stream()
                                // Retrieve lines matching with the range
                                .filter(entry -> entry.getKey() >= triple.getMiddle() && entry.getValue() <= triple.getRight())
                                .mapToLong(Map.Entry::getValue)
                                // If this expression spans multiple lines, get the maximum execution counts on those lines
                                .max()
                                .orElse(0L)))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
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
     * We unfortunately can't analyze the return type as we have not always resolved the method call (whereas we did
     * resolve the type) and therefore requires significant more effort to determine whether this method returns a
     * value or not. Therefore, cases such as toggle-methods (void methods without parameters) to toggle state in the
     * callee, can not be identified without significant additional effort.
     * <p>
     * Lastly, if there are any {@link MethodReferenceExpr} that do not reference to a getter-like method, we assume
     * the method call performs a modifiable operations on the target object. In such case we mark this relationship
     * as {@link DataRelationshipType#WRITE}. If none of these predicates match, we mark it as READ.
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

        return relevantNodes.stream()
                .filter(MethodReferenceExpr.class::isInstance)
                .map(MethodReferenceExpr.class::cast)
                .anyMatch(reference -> not(anyOf(
                        startsWithIgnoringCase("get"),
                        startsWithIgnoringCase("has"),
                        startsWithIgnoringCase("is")))
                        .matches(reference.getIdentifier())) ? WRITE : READ;
    }

    private void printResults(SourceAnalysisContext context,
                              List<VisitorResult> visitorResults,
                              Set<String> methodNameSet) {
        var counters = context.getCounters();
        // For unresolved AST nodes (method calls, object creations) we can not determine the class that was called.
        LOGGER.warn("Ignored {} AST nodes as they could not be resolved. Set property 'logging=debug' to see why they " +
                "could not be resolved.", counters.unresolvedNodes);

        LOGGER.info("Graph edges results:" +
                        "\n\tTotal constructor calls:           {}" +
                        "\n\tMatching constructor calls         {}" +
                        "\n\tRelevant constructor calls         {}" +
                        "\n\tTotal unique method names:         {}" +
                        "\n\tTotal method references:           {}" +
                        "\n\tMatching method references:        {}" +
                        "\n\tRelevant method references:        {}" +
                        "\n\tTotal method calls:                {}" +
                        "\n\tMatching method calls:             {}" +
                        "\n\tRelevant method calls:             {}" +
                        "\n\tTotal field accesses:              {}" +
                        "\n\tRelevant field accesses:           {}" +
                        "\n\tTotal static field accesses:       {}" +
                        "\n\tRelevant static field accesses:    {}" +
                        "\n\tTotal import statements:           {}" +
                        "\n\tMatching import statements:        {}" +
                        "\n\tRelevant import statements:        {}" +
                        "\n\tTotal dependence relationships:    {}" +
                        "\n\tOf which data relationships:       {}",
                visitorResults.stream().mapToInt(res -> res.getObjectCreationExpressions().size()).sum(),
                counters.matchingConstructorCalls,
                counters.relevantConstructorCalls,
                methodNameSet.size(),
                visitorResults.stream().mapToInt(res -> res.getMethodReferences().size()).sum(),
                counters.matchingMethodReferences,
                counters.relevantMethodReferences,
                visitorResults.stream().mapToInt(res -> res.getMethodCalls().size()).sum(),
                counters.matchingMethodCalls,
                counters.relevantMethodCalls,
                visitorResults.stream().mapToInt(res -> res.getFieldAccesses().size()).sum(),
                counters.relevantFieldAccesses,
                visitorResults.stream().mapToInt(res -> res.getStaticNameExpressions().size()).sum(),
                counters.relevantStaticExpressions,
                visitorResults.stream().mapToInt(res -> res.getRegularImports().size()).sum(),
                counters.matchingImportStatements,
                counters.relevantImportStatements,
                context.getResultBuilder().getClasses().stream()
                        .mapToLong(clazz -> clazz.getDependenceRelationships().size())
                        .sum(),
                context.getResultBuilder().getOtherClasses().stream()
                        .mapToLong(clazz -> clazz.getDataRelationships().size())
                        .sum()
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
