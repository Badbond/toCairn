package me.soels.thesis.analysis;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.types.ResolvedType;
import me.soels.thesis.model.AbstractClass;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the declaring class for the a {@link MethodCallExpr}.
 * <p>
 * The declaring class needs to be resolved using a {@link SymbolResolver} as in the AST, we don't know which class is
 * being called. There are a few numerous cases which result in different retrieval strategies.
 */
public class MethodCallDeclaringClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCallDeclaringClassResolver.class);
    private int totalCount, identifiedCount, errorCount, calleeCount = 0;

    Optional<Pair<AbstractClass, MethodCallExpr>> getDeclaringClass(MethodCallExpr methodCallExpr, List<AbstractClass> allClasses) {
        var foundCallee = tryGetUsingCompleteResolution(methodCallExpr)
                .or(() -> tryGetUsingChildNameResolution(methodCallExpr))
                .or(() -> tryGetUsingRecursion(methodCallExpr, allClasses));

        // TODO: Remove debug code.
        if (foundCallee.isEmpty()) {
            errorCount++;
        } else {
            identifiedCount++;
        }
        totalCount++;

        var result = foundCallee.flatMap(callee -> findByAbstractClass(allClasses, callee))
                .map(callee -> Pair.of(callee, methodCallExpr));
        result.ifPresent(res -> calleeCount++);
        return result;
    }

    /**
     * Try resolving the entire methodCallExpr to.
     * <p>
     * This allows allows sequential method calls (e.g. {@code a.callA().callB()}) to be resolved properly as the
     * return type is also resolved. Therefore, this should always be tried first. However, this approach is more
     * error prone as there are more places where symbol resolution could fail, such as the arguments provided.
     *
     * @param methodCallExpr the expression to fully resolve
     * @return an optional string with the FQN of the class in which the method called is declared
     */
    private Optional<String> tryGetUsingCompleteResolution(MethodCallExpr methodCallExpr) {
        try {
            var resolvedMethodCall = methodCallExpr.resolve();
            return Optional.of(resolvedMethodCall.getPackageName() + "." + resolvedMethodCall.getClassName());
        } catch (Exception e) {
            // Unfortunately, Java symbol resolver is not capable of resolving all method calls and is unmaintained.
            // Common problem are either based on their parents (the variable used to call a method) or its children
            // (resolving parameters). We only want to know which class is being called and therefore we retry with
            // only the variable. If that fails, we can not deduce the callee and we ignore this method call.
            LOGGER.debug("Could not resolve method " + methodCallExpr.getNameAsString() + " using full resolution", e);
            return Optional.empty();
        }
    }

    /**
     * Try resolving the methodCallExpr using a child {@link NameExpr}.
     * <p>
     * When full resolution fails, for example due to argument symbol solving errors, we can try to only resolve
     * the possibly available {@link NameExpr} in this call expression. This name expression can either be from a
     * variable (e.g. {@code variable.call()} or a class in case of static invocation (e.g. {@code Objects.equal()}).
     *
     * @param methodCallExpr the expression to fully resolve
     * @return an optional string with the FQN of the class in which the method called is declared
     */
    private Optional<String> tryGetUsingChildNameResolution(MethodCallExpr methodCallExpr) {
        try {
            return methodCallExpr.getChildNodes().stream()
                    .filter(childNode -> childNode instanceof NameExpr)
                    .map(childNode -> (NameExpr) childNode)
                    .findFirst()
                    .map(Expression::calculateResolvedType)
                    .map(ResolvedType::describe);
        } catch (Exception e) {
            LOGGER.debug("Could not resolve method " + methodCallExpr.getNameAsString() + " using resolution of the child's identifier", e);
            return Optional.empty();
        }
    }

    private Optional<String> tryGetUsingRecursion(MethodCallExpr methodCallExpr, List<AbstractClass> allClasses) {
        // TODO: Do something with this method. It should not happen as we traverse the tree in pre-order:
        //  prior methods should already be resolved IF POSSIBLE.
        return methodCallExpr.getChildNodes().stream()
                .filter(childNode -> childNode instanceof MethodCallExpr)
                .findFirst() // Can only have one method call as direct child
                .flatMap(childNode -> getDeclaringClass((MethodCallExpr) childNode, allClasses))
                .map(result -> result.getKey().getIdentifier());
    }

    private Optional<AbstractClass> findByAbstractClass(List<AbstractClass> allClasses, String fqn) {
        return allClasses.stream()
                .filter(clazz -> clazz.getIdentifier().equals(fqn))
                .findFirst();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getIdentifiedCount() {
        return identifiedCount;
    }

    public int getCalleeCount() {
        return calleeCount;
    }
}
