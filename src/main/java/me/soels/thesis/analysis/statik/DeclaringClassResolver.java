package me.soels.thesis.analysis.statik;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import me.soels.thesis.model.AbstractClass;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the declaring class for the a {@link MethodCallExpr}, {@link ObjectCreationExpr} or
 * {@link MethodReferenceExpr}.
 * <p>
 * The declaring class needs to be resolved using a {@link SymbolResolver} as in the AST, we don't know which class is
 * being called. There are a few numerous cases which result in different retrieval strategies.
 * <p>
 * If needed, this class can be extended with doing declaring class recovery through return types resolution of child
 * method calls or by doing manual analysis of identifying unique methods in the scope of the package and the class'
 * import statements, with the latter requiring significant work.
 */
@Service
public class DeclaringClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeclaringClassResolver.class);

    /**
     * Returns an optional result of the {@link AbstractClass} called as part of the given {@link ObjectCreationExpr}.
     * <p>
     * For convenience, we return a {@link Pair} of the found class and the AST node that we analyzed.
     *
     * @param context    the analysis context to retrieve or update context information with
     * @param node       the node to analyze
     * @param allClasses all the identified classes in the application
     * @return an optional result of the called class as part of the given constructor call
     */
    Optional<Pair<AbstractClass, ObjectCreationExpr>> resolveConstructorCall(StaticAnalysisContext context,
                                                                             ObjectCreationExpr node,
                                                                             List<AbstractClass> allClasses) {
        var resolvedConstructor = tryGetUsingCompleteResolution(node)
                .or(() -> tryResolveByCalculatingType(node));

        if (resolvedConstructor.isEmpty()) {
            context.getCounters().unresolvedNodes++;
        }

        return resolvedConstructor.flatMap(callee -> findByAbstractClass(allClasses, callee))
                .map(callee -> Pair.of(callee, node));
    }

    /**
     * Returns an optional result of the {@link AbstractClass} called as part of the given {@link MethodReferenceExpr}.
     * <p>
     * For convenience, we return a {@link Pair} of the found class and the AST node that we analyzed.
     *
     * @param context    the analysis context to retrieve or update context information with
     * @param node       the node to analyze
     * @param allClasses all the identified classes in the application
     * @return an optional result of the called class as part of the given method reference
     */
    Optional<Pair<AbstractClass, MethodReferenceExpr>> resolveMethodReference(StaticAnalysisContext context,
                                                                              MethodReferenceExpr node,
                                                                              List<AbstractClass> allClasses) {
        var resolvedMethodReference = tryGetUsingCompleteResolution(node)
                .or(() -> tryResolveByCalculatingType(node))
                .or(() -> tryGetUsingChildExpressionResolution(node, TypeExpr.class));

        if (resolvedMethodReference.isEmpty()) {
            context.getCounters().unresolvedNodes++;
        }

        return resolvedMethodReference.flatMap(callee -> findByAbstractClass(allClasses, callee))
                .map(callee -> Pair.of(callee, node));
    }

    /**
     * Returns an optional result of the {@link AbstractClass} called as part of the given {@link MethodCallExpr}.
     * <p>
     * For convenience, we return a {@link Pair} of the found class and the AST node that we analyzed.
     *
     * @param context    the analysis context to retrieve or update context information with
     * @param node       the node to analyze
     * @param allClasses all the identified classes in the application
     * @return an optional result of the called class as part of the given method call
     */
    Optional<Pair<AbstractClass, MethodCallExpr>> resolveMethodCall(StaticAnalysisContext context,
                                                                    MethodCallExpr node,
                                                                    List<AbstractClass> allClasses) {
        var foundCallee = tryGetUsingCompleteResolution(node)
                // Other cases include thisExpr (which we ignore because we don't allow for self reference) and
                // methodCallExpr (which we ignore because we already tried full resolution before)
                .or(() -> tryGetUsingChildExpressionResolution(node, NameExpr.class));

        if (foundCallee.isEmpty()) {
            context.getCounters().unresolvedNodes++;
        }

        return foundCallee.flatMap(callee -> findByAbstractClass(allClasses, callee))
                .map(callee -> Pair.of(callee, node));
    }

    /**
     * Try resolving the a {@link Resolvable} indicating a method-like (methods and constructors) declaration entirely.
     * <p>
     * Performing this action will resolve the node's children such as parameters in a method, but also the parents
     * such that the type of the node used can be determined. This decreases performance of the analysis drastically
     * but is an important step to fully cover all the nodes required for dependency construction.
     * <p>
     * Note that as this resolves more than just the type of the node, it is also more likely to fail if not all
     * information is present or acquirable. To increase chances of resolving using this method, one should also
     * provide the dependency {@code .jar} files so that entire ASTs can be resolved. This decreases performance even
     * more but provides a more complete analysis.
     * <p>
     * This, for example, allows sequential method calls (e.g. {@code a.callA().callB()}) to be resolved properly as
     * the return type is also resolved. Therefore, this should always be tried first. However, this approach is more
     * error prone as there are more places where symbol resolution could fail, such as the arguments provided. This
     * happens for example with argument types that have been generated compile-time.
     *
     * @param node the expression to fully resolve
     * @return an optional string with the FQN of the class in which the method called is declared
     */
    private Optional<String> tryGetUsingCompleteResolution(Resolvable<? extends ResolvedMethodLikeDeclaration> node) {
        try {
            var resolvedNode = node.resolve();
            return Optional.of(resolvedNode.getPackageName() + "." + resolvedNode.getClassName());
        } catch (Exception e) {
            LOGGER.trace("Could not resolve node " + node + " using full resolution", e);
            return Optional.empty();
        }
    }

    /**
     * Try resolving the node using a child {@link Expression}.
     * <p>
     * When full resolution fails, for example due to argument symbol solving errors, we can try to only resolve a
     * child expression (e.g. {@link NameExpr} in {@link MethodCallExpr} or {@link TypeExpr} in
     * {@link MethodReferenceExpr}).
     * <p>
     * This name expression in a method call can either be from a variable (e.g. {@code variable.call()} or a class in
     * case of static invocation (e.g. {@code Objects.equal()}).<p>
     * <p>
     * Cases in which this generally fails is when the class could not be resolved due to it coming from libraries
     * that are part of another {@code .jar} or when the referenced class is generated compile-time. In the first case,
     * we can safely ignore it as that referenced class is not part of the clustering. The second case is very difficult
     * to resolve without compiling the source code and therefore the project should be provided with generated sources.
     * Lastly, this can fail if the class can not be resolved due to reflection usage.
     *
     * @param node               the node to resolve
     * @param expectedExpression the expression class to expect
     * @return an optional string with the FQN of the class in which the method called is declared
     */
    private Optional<String> tryGetUsingChildExpressionResolution(Expression node, Class<? extends Expression> expectedExpression) {
        return node.getChildNodes().stream()
                .filter(child -> expectedExpression.isAssignableFrom(child.getClass()))
                .map(expectedExpression::cast)
                .findFirst()
                .flatMap(this::tryResolveByCalculatingType);
    }

    /**
     * Try to resolve the the {@link Expression} by calculating its resolved type.
     * <p>
     * This will determine the type in the scope of the expression AST node. This does not work for
     * {@link MethodCallExpr} as the type of such expression is not the callee class. However, with
     * {@link ObjectCreationExpr} and {@link MethodReferenceExpr}, the calculated type is the callee itself.
     *
     * @param node the node to analyze
     * @return an optional result of the called class as part of the given expression
     */
    private Optional<String> tryResolveByCalculatingType(Expression node) {
        try {
            var type = node.calculateResolvedType();
            return Optional.of(type.describe());
        } catch (Exception e) {
            getSymbolErrorMessage(e).ifPresent(message -> LOGGER.debug("{} for expression {}", message, node));
            LOGGER.trace("Could not resolve expression " + node + " by resolving its type", e);
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
     * @param fqn        the fully qualified name to search for
     * @return the optional class related to the fully qualified name
     */
    private Optional<AbstractClass> findByAbstractClass(List<AbstractClass> allClasses, String fqn) {
        return allClasses.stream()
                .filter(clazz -> clazz.getIdentifier().equals(fqn))
                .findFirst();
    }
}
