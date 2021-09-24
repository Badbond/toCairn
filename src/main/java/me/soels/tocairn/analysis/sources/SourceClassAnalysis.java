package me.soels.tocairn.analysis.sources;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.tocairn.model.AbstractClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.soels.tocairn.util.StringContainsIgnoreCaseMatcher.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.anyOf;

/**
 * Performs source analysis on the provided project to determine the classes defined in that project.
 */
@Service
public class SourceClassAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceClassAnalysis.class);
    private static final String CONTROLLER = "controller";

    public void analyze(SourceAnalysisContext context) {
        LOGGER.info("Extracting classes");
        var start = System.currentTimeMillis();
        var filterCount = new MutableInt(0);
        var config = new ParserConfiguration().setLanguageLevel(context.getInput().getLanguageLevel());

        var typesAndClasses = new SymbolSolverCollectionStrategy(config)
                .collect(context.getProjectLocation()).getSourceRoots().stream()
                // Don't include test directories (ideally, they were already filtered out by the user)
                .filter(root -> !root.getRoot().toString().contains("/test/"))
                // Parse the source roots, resolving the class types. We do not parallelize as that gave errors sometimes.
                // Print any problems that occur and filter those cases out.
                .flatMap(root -> parseRoot(root).stream())
                .map(this::printProblems)
                .filter(Objects::nonNull)
                .filter(parseResult -> parseResult.getResult().isPresent())
                .map(parseResult -> parseResult.getResult().get())
                .flatMap(cu -> cu.getTypes().stream())
                // Also include the inner types, note we do not include annotations and enums
                .flatMap(type -> type.findAll(ClassOrInterfaceDeclaration.class).stream())
                // Print problems where FQN could not be determined and filter those cases out
                .map(this::printEmptyQualifiers)
                .filter(Objects::nonNull)
                .filter(clazz -> clazz.getFullyQualifiedName().isPresent())
                // Only include classes matching the filter
                .filter(clazz -> filterIncludeClassBasedOnPathRegex(clazz, context.getInput().getPathIncludeRegexes(), filterCount))
                // Exclude classes based on input regexes matching FQN
                .filter(clazz -> filterExcludeClassBasedOnFQNRegex(clazz, context.getInput().getFqnExcludeRegexes(), filterCount))
                // Create a pair of the type of class and its AST
                .map(clazz -> Pair.of(clazz, storeClass(clazz, context.getInput(), context, getStorageLocation(clazz))))
                .filter(pair -> pair.getValue() != null)
                .collect(Collectors.toList());
        context.setTypesAndClasses(typesAndClasses);

        LOGGER.info("Graph nodes results:" +
                        "\n\tClasses filtered out:      {}" +
                        "\n\tTotal classes:             {}" +
                        "\n\tData classes:              {}" +
                        "\n\tOther classes:             {}",
                filterCount.getValue(),
                context.getResultBuilder().getClasses().size(),
                context.getResultBuilder().getDataClasses().size(),
                context.getResultBuilder().getOtherClasses().size());
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Source class analysis took {} (H:m:s.millis)", duration);
    }

    private boolean filterIncludeClassBasedOnPathRegex(ClassOrInterfaceDeclaration clazz, List<String> pathIncludeRegexes, MutableInt filterCount) {
        if (pathIncludeRegexes.isEmpty()) {
            // When not providing a regex, we allow all classes.
            return true;
        }

        var path = clazz.findCompilationUnit()
                .flatMap(CompilationUnit::getStorage)
                .map(CompilationUnit.Storage::getPath);
        if (path.isEmpty()) {
            LOGGER.warn("Clazz with name {} has no compilation unit or storage associated", clazz.getNameAsString());
            filterCount.increment();
            return false;
        }

        var result = pathIncludeRegexes.stream().anyMatch(regex -> path.get().toString().matches(regex));
        if (!result) {
            filterCount.increment();
        }
        return result;
    }

    private boolean filterExcludeClassBasedOnFQNRegex(ClassOrInterfaceDeclaration clazz, List<String> fqnExcludeRegexes, MutableInt count) {
        if (fqnExcludeRegexes.stream().anyMatch(regex -> clazz.getFullyQualifiedName().get().matches(regex))) {
            count.increment();
            return false;
        }
        return true;
    }

    private String getStorageLocation(ClassOrInterfaceDeclaration clazz) {
        return clazz.findCompilationUnit()
                .flatMap(CompilationUnit::getStorage)
                .map(storage -> storage.getPath().toString())
                .orElse(null);
    }

    private AbstractClass storeClass(ClassOrInterfaceDeclaration clazz, SourceAnalysisInput input, SourceAnalysisContext context,
                                     String location) {
        var fqn = clazz.getFullyQualifiedName()
                .orElseThrow(() -> new IllegalStateException("Could not retrieve FQN from already filtered class"));
        var featureSet = getFeatureSet(input, clazz);

        if (isDataClass(clazz, input)) {
            // Use line count as initial size of the data class, to be overridden by dynamic analysis with more accurate
            // size calculations.
            return context.getResultBuilder().addDataClass(fqn, clazz.getNameAsString(), location, featureSet);
        } else {
            var isAPIClass = clazz.getNameAsString().toLowerCase().contains(CONTROLLER) ||
                    featureSet.stream().anyMatch(feature ->
                            feature.toLowerCase().contains(CONTROLLER) ||
                                    feature.toLowerCase().contains("api")) ||
                    clazz.getAnnotations().stream()
                            .map(NodeWithName::getNameAsString)
                            .anyMatch(ann -> ann.toLowerCase().contains(CONTROLLER));
            var isExecutedAPIClass = isAPIClass &&
                    context.getSourceExecutions().get(fqn) != null &&
                    context.getSourceExecutions().get(fqn).values().stream().anyMatch(value -> value > 0);
            return context.getResultBuilder().addOtherClass(fqn, clazz.getNameAsString(), location, featureSet, clazz.getMethods().size(), isExecutedAPIClass);
        }
    }

    private Set<String> getFeatureSet(SourceAnalysisInput input, ClassOrInterfaceDeclaration clazz) {
        var packageName = clazz.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(NodeWithName::getNameAsString)
                .orElse("");
        var matcher = Pattern.compile("^(" + input.getTopPackageRegex() + ")(.*)$")
                .matcher(packageName);
        if (matcher.matches()) {
            // Get last group per our regex and set the package to that
            packageName = matcher.group(matcher.groupCount());
        }

        return Arrays.stream(packageName.split("\\."))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    private boolean isDataClass(ClassOrInterfaceDeclaration clazz, SourceAnalysisInput input) {
        return input.getDataClassFqnRegexes().stream()
                .anyMatch(regex -> clazz.getFullyQualifiedName().get().matches(regex)) ||
                classNameIndicatesDataStructure(clazz) ||
                classContainsDataAnnotation(clazz, input);
    }

    private boolean classNameIndicatesDataStructure(ClassOrInterfaceDeclaration clazz) {
        // There are more names which would indicate data (such as 'data', 'model', etc.) but those might also return
        // false positives, for example: ModellingService, DataController.
        return StringUtils.endsWithIgnoreCase(clazz.getNameAsString(), "DTO") ||
                StringUtils.endsWithIgnoreCase(clazz.getNameAsString(), "DAO");
    }

    private boolean classContainsDataAnnotation(ClassOrInterfaceDeclaration clazz, SourceAnalysisInput input) {
        return clazz.getAnnotations().stream()
                .map(NodeWithName::getNameAsString)
                .anyMatch(annotation -> anyOf(containsStringIgnoringCase("immutable"),
                        containsStringIgnoringCase("entity"),
                        containsStringIgnoringCase(input.getCustomDataAnnotation()))
                        .matches(annotation));
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
