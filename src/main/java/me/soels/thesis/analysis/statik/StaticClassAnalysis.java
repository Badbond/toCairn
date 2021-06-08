package me.soels.thesis.analysis.statik;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static me.soels.thesis.util.StringContainsIgnoreCaseMatcher.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.anyOf;

/**
 * Performs static analysis on the provided project to determine the classes defined in that project.
 */
@Service
public class StaticClassAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticClassAnalysis.class);

    public void analyze(StaticAnalysisContext context) {
        LOGGER.info("Extracting classes");
        var start = System.currentTimeMillis();
        var config = new ParserConfiguration().setLanguageLevel(context.getInput().getLanguageLevel());

        /**
         *TODO: Investigate and solve the problems faced by the ReflectionTypeSolver
         * As they document in their Javadoc, classes in OUR application's classpath will be included in the analysis.
         * This means that, now that we introduced Spring, we include many of our own classes that AST nodes will be
         * resolved against. This has the downside of having worse performance (mainly because this solver will always
         * be checked first before the 'source root' class solvers (which matter most).
         * <p>
         * However, the biggest problems is conflicting with libraries used in the application analyzed and our own
         * thesis application. Currently, the analysis fails on 'FilterRegistrationBean'. Of the analyzed application,
         * this is referring to a Spring boot 2.4.5 dependency (which is not included in the type solvers because we
         * don't include dependencies because of performance reasons). However, our Spring boot 2.5.0 version of the
         * class is picked up instead (prior to us introducing Spring, this just failed as it could not be resolved).
         * This class can not be parsed correctly because we don't have javax.servlet on our classpath (not sure why
         * we don't) whereas at the analyzed application they do using jakarta.servlet-api dependency (but I can't see
         * where it is declared).
         * <p>
         * We should not want to just omit this solver because many 'java' and 'javax' packages are very useful to
         * solve allowing for a more complete analysis. For example, Objects.requireNonNull(T) is easily resolvable
         * and allows us to continue resolving the provided argument to determine the actual type being returned.
         * <p>
         * We can influence the solvers using the ParserConfiguration above by using 'setSymbolResolver()'. However,
         * it appears that upon doing so, the 'SymbolSolverCollectionStrategy' is not capable of injecting additional
         * solvers that are generated while finding source roots. Even if we use the same solver as defined in
         * SymbolSolverCollectionStrategy. These JavaParserTypeSolvers are most important in analysis to match classes
         * within the application to analyze. For that matter, I have not been able to test whether the jreOnly
         * argument on ReflectionTypeSolver would work.
         * <p>
         * I've also looked into catching the ClassDefNotFoundError. That might be a viable solution; completely
         * ignoring the node that we can't resolve. Previously (before Spring) we could also not resolve it as we did
         * not have the class on the classpath nor in the other solvers (as we don't include libraries). However,
         * I would like to, once the model is final/mature, to run the analysis with libraries (which would likely take
         * hours/days) in order to resolve more nodes and therefore get a more complete graph. The problem that we are
         * facing now would still be present as ReflectionTypeSolver is the first solver.
         **/
        new SymbolSolverCollectionStrategy(config)
                .collect(context.getProjectLocation()).getSourceRoots().stream()
                // Don't include test directories (ideally, they were already filtered out by the user)
                .filter(root -> !root.getRoot().toString().contains("/test/"))
                // Parse the source roots, resolving the class types. We do not parallelize as that gave errors sometimes.
                // Print any problems that occur and filter those cases out.
                .flatMap(root -> parseRoot(root).stream())
                .map(this::printProblems)
                .filter(Objects::nonNull)
                .filter(parseResult -> parseResult.getResult().isPresent())
                .flatMap(parseResult -> parseResult.getResult().get().getTypes().stream())
                // Also include the inner types, note we do not include annotations and enums
                .flatMap(type -> type.findAll(ClassOrInterfaceDeclaration.class).stream())
                // Print problems where FQN could not be determined and filter those cases out
                .map(this::printEmptyQualifiers)
                .filter(Objects::nonNull)
                .filter(clazz -> clazz.getFullyQualifiedName().isPresent())
                // Create a pair of the type of class and its AST
                .forEach(clazz -> storeClass(clazz, context.getInput(), context));

        LOGGER.info("Graph nodes results:" +
                        "\n\tTotal classes:       {}" +
                        "\n\tData classes:        {}" +
                        "\n\tOther classes:       {}",
                context.getResultBuilder().getAllClasses().size(),
                context.getResultBuilder().getDataClasses().size(),
                context.getResultBuilder().getOtherClasses().size());
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static class analysis took {} (H:m:s.millis)", duration);
    }

    private void storeClass(ClassOrInterfaceDeclaration clazz, StaticAnalysisInput input, StaticAnalysisContext context) {
        var fqn = clazz.getFullyQualifiedName()
                .orElseThrow(() -> new IllegalStateException("Could not retrieve FQN from already filtered class"));

        if (isDataClass(clazz, input)) {
            // TODO: Replace 1 size with default in case dynamic analysis has not seen data class
            context.getResultBuilder().addDataClass(fqn, clazz.getNameAsString(), 1);
        } else {
            context.getResultBuilder().addOtherClass(fqn, clazz.getNameAsString());
        }
    }

    private boolean isDataClass(ClassOrInterfaceDeclaration clazz, StaticAnalysisInput input) {
        // TODO: Check for usage of JsonProperty/JsonCreator perhaps?
        return clazzNameIndicatesDataStructure(clazz) ||
                clazzContainsDataAnnotation(clazz, input) ||
                clazzLooksLikeDataStructure(clazz);
    }

    private boolean clazzNameIndicatesDataStructure(ClassOrInterfaceDeclaration clazz) {
        // There are more names which would indicate data (such as 'data', 'model', etc.) but those might also return
        // false positives, for example: ModellingService, DataController.
        return StringUtils.endsWithIgnoreCase(clazz.getNameAsString(), "DTO") ||
                StringUtils.endsWithIgnoreCase(clazz.getNameAsString(), "DAO");
    }

    private boolean clazzContainsDataAnnotation(ClassOrInterfaceDeclaration clazz, StaticAnalysisInput input) {
        return clazz.getAnnotations().stream()
                .map(NodeWithName::getNameAsString)
                .anyMatch(annotation -> anyOf(containsStringIgnoringCase("immutable"),
                        containsStringIgnoringCase("entity"),
                        containsStringIgnoringCase(input.getCustomDataAnnotation()))
                        .matches(annotation));
    }

    private boolean clazzLooksLikeDataStructure(ClassOrInterfaceDeclaration clazz) {
        // TODO: Look at class structure
        return false;
    }

    private List<ParseResult<CompilationUnit>> parseRoot(SourceRoot root) {
        try {
            return root.tryToParse();
        } catch (IOException e) {
            throw new IllegalStateException("Could not process one of the file in source root " + root.getRoot(), e);
        }
    }

    private ClassOrInterfaceDeclaration printEmptyQualifiers(ClassOrInterfaceDeclaration typeDeclaration) {
        if (typeDeclaration.getFullyQualifiedName().isEmpty()) {
            // This could happen with inner types, but as we do symbol solving, it should not happen.
            // In any case, we will exclude these from the analysis as we can not uniquely identify them in the graph.
            LOGGER.warn("Could not construct FQN for type {}. Skipping it.", typeDeclaration.getNameAsString());
            return null;
        }

        return typeDeclaration;
    }

    private ParseResult<CompilationUnit> printProblems(ParseResult<CompilationUnit> parseResult) {
        if (!parseResult.isSuccessful()) {
            var problemString = parseResult.getProblems().stream()
                    .reduce("", (str, problem) -> str + "\n\t" + problem.getVerboseMessage(), (str1, str2) -> str1 + str2);
            parseResult.getResult().flatMap(CompilationUnit::getStorage).ifPresentOrElse(
                    storage -> LOGGER.warn("Problem(s) in parse result for file {}:{}", storage.getFileName(), problemString),
                    () -> LOGGER.warn("Problem(s) in unknown parse result:{}", problemString)
            );
            return null;
        }

        return parseResult;
    }
}
