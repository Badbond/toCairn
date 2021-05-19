package me.soels.thesis.analysis;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
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
    private int totalCount = 0;
    private int identifiedCount = 0;
    private int errorCount = 0;
    private int calleeCount = 0;

    Optional<Pair<AbstractClass, MethodCallExpr>> getDeclaringClass(MethodCallExpr methodCallExpr, List<AbstractClass> allClasses) {
        var foundCallee = tryGetUsingCompleteResolution(methodCallExpr)
                .or(() -> tryGetUsingChildNameResolution(methodCallExpr));

        // TODO: Remove debug code or make nicer.
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
     * Try resolving the entire methodCallExpr.
     * <p>
     * This allows allows sequential method calls (e.g. {@code a.callA().callB()}) to be resolved properly as the
     * return type is also resolved. Therefore, this should always be tried first. However, this approach is more
     * error prone as there are more places where symbol resolution could fail, such as the arguments provided. This
     * happens for example with argument types that have been generated compile-time.
     *
     * @param methodCallExpr the expression to fully resolve
     * @return an optional string with the FQN of the class in which the method called is declared
     */
    private Optional<String> tryGetUsingCompleteResolution(MethodCallExpr methodCallExpr) {
        try {
            var resolvedMethodCall = methodCallExpr.resolve();
            return Optional.of(resolvedMethodCall.getPackageName() + "." + resolvedMethodCall.getClassName());
        } catch (Exception e) {
            LOGGER.trace("Could not resolve method " + methodCallExpr.getNameAsString() + " using full resolution", e);
            return Optional.empty();
        }
    }

    /**
     * Try resolving the methodCallExpr using a child {@link NameExpr}.
     * <p>
     * When full resolution fails, for example due to argument symbol solving errors, we can try to only resolve
     * the possibly available {@link NameExpr} in this call expression. This name expression can either be from a
     * variable (e.g. {@code variable.call()} or a class in case of static invocation (e.g. {@code Objects.equal()}).
     * <p>
     * Cases in which this generally fails is when the class could not be resolved due to it coming from libraries
     * that are part of another {@code .jar} or when the referenced class is generated compile-time. In the first case,
     * we can safely ignore it as that referenced class is not part of the clustering. The second case is very difficult
     * to resolve without compiling the source code.
     *
     * @param methodCallExpr the expression to fully resolve
     * @return an optional string with the FQN of the class in which the method called is declared
     */
    private Optional<String> tryGetUsingChildNameResolution(MethodCallExpr methodCallExpr) {
        try {
            return methodCallExpr.getChildNodes().stream()
                    .filter(NameExpr.class::isInstance)
                    .map(NameExpr.class::cast)
                    .findFirst()
                    .map(Expression::calculateResolvedType)
                    .map(ResolvedType::describe);
        } catch (Exception e) {
            getSymbolErrorMessage(e).ifPresent(message -> LOGGER.debug("{} for expression {}", message, methodCallExpr));
            LOGGER.trace("Could not resolve method " + methodCallExpr.getNameAsString() + " using resolution of the child's identifier", e);
            return Optional.empty();
        }
    }

    private Optional<String> getSymbolErrorMessage(Throwable e) {
        if (e instanceof UnsolvedSymbolException) {
            return Optional.of(e.getMessage());
        } else if (e.getCause() != null) {
            return getSymbolErrorMessage(e.getCause());
        } else {
            return Optional.empty();
        }
    }

    // TODO: Another possibility would be to resolve the return type of child MethodCallExpr

    // TODO: Another possibility would be to check all possible accessible classes based on same package + imports and
    //  see if there is a unique method that we can match

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
