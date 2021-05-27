package me.soels.thesis.analysis;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.DataClass;
import me.soels.thesis.model.OtherClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

/**
 * Performs static analysis on the provided project to determine the classes defined in that project.
 */
public class StaticClassAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticClassAnalysis.class);

    public void analyze(StaticAnalysisContext context) {
        LOGGER.info("Extracting classes");
        var start = System.currentTimeMillis();

        var config = new ParserConfiguration().setLanguageLevel(context.getInput().getLanguageLevel());
        var allTypes = new SymbolSolverCollectionStrategy(config)
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
                .map(clazz -> Pair.of(identifyClass(clazz, context.getInput()), clazz))
                .collect(Collectors.toList());
        context.addClassesAndTypes(allTypes);

        LOGGER.info("Graph nodes results:" +
                        "\n\tTotal classes:       {}" +
                        "\n\tData classes:        {}" +
                        "\n\tOther classes:       {}",
                allTypes.size(), context.getDataClasses().size(), context.getOtherClasses().size());
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static class analysis took {} (H:m:s.millis)", duration);
    }

    private AbstractClass identifyClass(ClassOrInterfaceDeclaration clazz, StaticAnalysisInput input) {
        var fqn = clazz.getFullyQualifiedName()
                .orElseThrow(() -> new IllegalStateException("Could not retrieve FQN from already filtered class"));

        if (isDataClass(clazz, input)) {
            return new DataClass(fqn, clazz.getNameAsString(), null);
        } else {
            return new OtherClass(fqn, clazz.getNameAsString());
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
            // This usually happens with inner types, but as we do symbol solving, it should not happen.
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
