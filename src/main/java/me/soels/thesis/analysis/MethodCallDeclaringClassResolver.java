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
 * <p>
 * If needed, this class can be extended with doing declaring class recovery through return types resolution of child
 * method calls or by doing manual analysis of identifying unique methods in the scope of the package and the class'
 * import statements, with the latter requiring significant work.
 */
public class MethodCallDeclaringClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCallDeclaringClassResolver.class);

    /**
     * Returns an optional result of the {@link AbstractClass} called as part of the given {@link MethodCallExpr}.
     * <p>
     * For convenience, we return a {@link Pair} of the found class and the method call that we analyzed.
     *
     * @param context        the analysis context to retrieve or update context information with
     * @param methodCallExpr the call to analyze
     * @param allClasses     all the identified classes in the application
     * @return an optional result of the called class as part of the given method call
     */
    Optional<Pair<AbstractClass, MethodCallExpr>> getDeclaringClass(StaticAnalysisContext context,
                                                                    MethodCallExpr methodCallExpr,
                                                                    List<AbstractClass> allClasses) {
        // We have already filtered on the method being in the set of declared methods in the project
        context.getCounters().matchingMethodCalls++;

        var foundCallee = tryGetUsingCompleteResolution(methodCallExpr)
                .or(() -> tryGetUsingChildNameResolution(methodCallExpr));

        if (foundCallee.isEmpty()) {
            context.getCounters().unresolvedNodes++;
        }

        return foundCallee.flatMap(callee -> findByAbstractClass(allClasses, callee))
                .map(callee -> Pair.of(callee, methodCallExpr));
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

    /**
     * Unwraps the given {@link Throwable} to retrieve the possible message of an {@link UnsolvedSymbolException}.
     *
     * @param e the throwable to unwrap
     * @return an optional message indicating why the resolution did not succeed
     */
    private Optional<String> getSymbolErrorMessage(Throwable e) {
        if (e instanceof UnsolvedSymbolException) {
            return Optional.of(e.getMessage());
        } else if (e.getCause() != null) {
            return getSymbolErrorMessage(e.getCause());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Finds a {@link AbstractClass} by the given fully qualified name.
     *
     * @param allClasses the application's classes to search in
     * @param fqn the fully qualified name to search for
     * @return the optional class related to the fully qualified name
     */
    private Optional<AbstractClass> findByAbstractClass(List<AbstractClass> allClasses, String fqn) {
        return allClasses.stream()
                .filter(clazz -> clazz.getIdentifier().equals(fqn))
                .findFirst();
    }
}
