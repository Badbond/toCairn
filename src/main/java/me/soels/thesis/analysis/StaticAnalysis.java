package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.thesis.analysis.StringContainsIgnoreCaseMatcher.containsIgnoringCase;
import static me.soels.thesis.analysis.ZipExtractor.extractZip;
import static me.soels.thesis.model.DataRelationshipType.READ;
import static me.soels.thesis.model.DataRelationshipType.WRITE;
import static org.hamcrest.CoreMatchers.anyOf;

/**
 * Performs static analysis of the given {@code .zip} file containing the application's source files.
 * <p>
 * With static analysis, we build the model that contains the data classes identified in the application, the other
 * classes containing business logic, relationships between these other classes and relationships between other classes
 * and data classes.
 * <p>
 * We use {@link JavaParser} for building the abstract syntax tree and resolving the type references. Some of this class
 * is based on their book <i>'JavaParser: Visited'</i>. Please note that we do not introduce any concurrency ourselves
 * on purpose as the library used returns more errors when doing so (both during parsing classes and resolving methods).
 * <p>
 * Note that this static analysis does not allow for resolving relations that are constructed at runtime. Therefore,
 * it does not allow to represent injection and polymorphism relations. We can mitigate this if desired using dynamic
 * analysis.
 * <p>
 * TODO: Perhaps we do need to include dependencies as those dependencies can also model data. Then we would still
 * be in favour of combining classes that use the same data models in terms of data autonomy (even though the data
 * is modeled in the dependency. An example of this is FcId.class in shared library of host org.
 */
public class StaticAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAnalysis.class);
    // TODO: Would rather have injection set up to inject the service instead.
    private final MethodCallDeclaringClassResolver declaringClassResolver = new MethodCallDeclaringClassResolver();
    private int count = 0;

    // TODO: Split in class analysis and relationship analysis.
    public void analyze(AnalysisModelBuilder modelBuilder, StaticAnalysisInput input) {
        LOGGER.info("Starting static analysis on {}", input.getPathToZip());

        var inputZip = input.getPathToZip();
        if (!inputZip.getFileName().toString().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("The path does not refer to a .zip file, for path " + inputZip);
        } else if (!Files.exists(inputZip)) {
            throw new IllegalArgumentException("The zip file does not exist for path " + inputZip);
        }

        var projectLocation = extractZip(inputZip);
        performAnalysis(modelBuilder, projectLocation, input);
    }

    private void performAnalysis(AnalysisModelBuilder modelBuilder, Path projectLocation, StaticAnalysisInput input) {
        var start = System.currentTimeMillis();

        LOGGER.info("Extracting classes");
        var allTypes = getAllTypes(projectLocation, input);
        var allClasses = allTypes.stream().map(Pair::getKey).collect(Collectors.toList());
        var allMethodNames = allTypes.stream()
                .flatMap(pair -> pair.getValue().findAll(MethodDeclaration.class).stream())
                .map(NodeWithSimpleName::getNameAsString)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        var otherClasses = filterClasses(allTypes, OtherClass.class);
        var dataClasses = filterClasses(allTypes, DataClass.class);
        LOGGER.info("Graph nodes results:" +
                        "\n\tTotal classes:       {}" +
                        "\n\tData classes:        {}" +
                        "\n\tOther classes:       {}" +
                        "\n\tUnique method names: {}",
                allClasses.size(), dataClasses.size(), otherClasses.size(), allMethodNames.size());

        LOGGER.info("Extracting relationships");
        var allRelationships = allTypes.stream()
                // Only calculate relationships from service classes
                .filter(pair -> pair.getKey() instanceof OtherClass)
                .flatMap(pair -> this.resolveClassDependencies((OtherClass) pair.getKey(), allClasses, allMethodNames, pair.getValue()).stream())
                .collect(Collectors.toList());

        var dataRelationships = filterRelationships(allRelationships, DataRelationship.class);
        var classDependencies = filterRelationships(allRelationships, DependenceRelationship.class);

        LOGGER.info("Graph edges results:" +
                        "\n\tTotal matching method calls:   {}" +
                        "\n\tUnresolved calls:              {}" +
                        "\n\tResolved calls:                {}" +
                        "\n\tTo classes within application: {}" +
                        "\n\tExcluding self-reference:      {}" +
                        "\n\tTotal relationships:           {}" +
                        "\n\tData relationships:            {}" +
                        "\n\tOther class dependencies:      {}",
                declaringClassResolver.getTotalCount(), declaringClassResolver.getErrorCount(),
                declaringClassResolver.getIdentifiedCount(), declaringClassResolver.getCalleeCount(),
                count, allRelationships.size(), dataRelationships.size(), classDependencies.size());

        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Static analysis took {} (H:m:s.millis)", duration);

        modelBuilder.withDataClasses(dataClasses)
                .withOtherClasses(otherClasses)
                .withDependencies(classDependencies)
                .withDataRelationships(dataRelationships);
    }

    private List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> getAllTypes(Path projectLocation, StaticAnalysisInput input) {
        var config = new ParserConfiguration().setLanguageLevel(input.getLanguageLevel());
        return new SymbolSolverCollectionStrategy(config)
                .collect(projectLocation).getSourceRoots().stream()
                // Don't include test directories
                .filter(root -> !root.getRoot().toString().contains("/test/"))
                // Parse the source roots, resolving the class types. We do not parallelize as that gave errors sometimes.
                .flatMap(root -> parseRoot(root).stream())
                // Print problems, filter those resulting in no parse result, and retrieve the types defined in the result
                .map(this::printProblems)
                .filter(parseResult -> parseResult.getResult().isPresent())
                .flatMap(parseResult -> parseResult.getResult().get().getTypes().stream())
                // Also include the inner types, note we do not include annotations and enums
                .flatMap(type -> type.findAll(ClassOrInterfaceDeclaration.class).stream())
                // Print problems where FQN could not be determined and filter those cases out
                .map(this::printEmptyQualifiers)
                .filter(clazz -> clazz.getFullyQualifiedName().isPresent())
                // Create a pair of the type of class and its AST
                .map(clazz -> Pair.of(identifyClass(clazz, input), clazz))
                .collect(Collectors.toList());
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
                .anyMatch(annotation -> anyOf(containsIgnoringCase("immutable"),
                        containsIgnoringCase("entity"),
                        containsIgnoringCase(input.getCustomDataAnnotation()))
                        .matches(annotation));
    }

    private boolean clazzLooksLikeDataStructure(ClassOrInterfaceDeclaration clazz) {
        // TODO: Look at class structure
        return false;
    }

    private List<Relationship> resolveClassDependencies(OtherClass caller, List<AbstractClass> allClasses, List<String> allMethodNames, ClassOrInterfaceDeclaration classDeclaration) {
        // We need to iterate this way to resolve statements in a preorder for multiple types of nodes
        return classDeclaration.findAll(MethodCallExpr.class).stream()
                // Filter out method names that are definitely not within the application
                .filter(method -> allMethodNames.contains(method.getNameAsString()))
                // Try to resolve the method call to get the pair of callee and its method invoked.
                .flatMap(method -> declaringClassResolver.getDeclaringClass(method, allClasses).stream())
                // Filter out self invocation
                .filter(calleePair -> !calleePair.getKey().getIdentifier().equals(caller.getIdentifier()))
                // Group by callee class based on FQN
                .collect(groupingBy(Pair::getKey))
                .values().stream()
                // For every callee, identify the relationship
                .map(calleePairs -> identifyRelationship(caller, calleePairs))
                .collect(Collectors.toList());
    }

    private Relationship identifyRelationship(OtherClass clazz, List<Pair<AbstractClass, MethodCallExpr>> calleeMethods) {
        count += calleeMethods.size();
        var target = calleeMethods.get(0).getKey();
        var methods = calleeMethods.stream().map(Pair::getValue).collect(Collectors.toList());

        if (target instanceof DataClass) {
            return new DataRelationship(clazz, (DataClass) target, identifyReadWrite(methods), calleeMethods.size());
        } else if (target instanceof OtherClass) {
            return new DependenceRelationship(clazz, (OtherClass) target, calleeMethods.size());
        } else {
            throw new IllegalStateException("Could not yet support class type " + target.getClass().getSimpleName());
        }
    }

    private DataRelationshipType identifyReadWrite(List<MethodCallExpr> methodsCalled) {
        // When our caller has to provide arguments to the callee, we assume it is modifying the state of the callee
        // with those values.
        return methodsCalled.stream().anyMatch(method -> method.getArguments().size() > 1) ? WRITE : READ;
    }

    private List<ParseResult<CompilationUnit>> parseRoot(SourceRoot root) {
        try {
            return root.tryToParse();
        } catch (IOException e) {
            throw new IllegalStateException("Could not process one of the file in source root " + root.getRoot(), e);
        }
    }

    private <T extends AbstractClass> List<T> filterClasses(List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> allClasses, Class<T> expectedClass) {
        return allClasses.stream()
                .filter(pair -> expectedClass.isAssignableFrom(pair.getKey().getClass()))
                .map(pair -> expectedClass.cast(pair.getKey()))
                .collect(Collectors.toList());
    }

    private <T extends Relationship> List<T> filterRelationships(List<Relationship> relationships, Class<T> expectedClass) {
        return relationships.stream()
                .filter(relationship -> expectedClass.isAssignableFrom(relationship.getClass()))
                .map(expectedClass::cast)
                .collect(Collectors.toList());
    }

    private ClassOrInterfaceDeclaration printEmptyQualifiers(ClassOrInterfaceDeclaration typeDeclaration) {
        if (typeDeclaration.getFullyQualifiedName().isEmpty()) {
            // This usually happens with inner types, but as we do symbol solving, it should not happen.
            // In any case, we will exclude these from the analysis as we can not uniquely identify them in the graph.
            LOGGER.warn("Could not construct FQN for type {}. Skipping it.", typeDeclaration.getNameAsString());
        }

        return typeDeclaration;
    }

    private ParseResult<CompilationUnit> printProblems(ParseResult<CompilationUnit> parseResult) {
        if (!parseResult.isSuccessful()) {
            var problemString = parseResult.getProblems().stream()
                    .reduce("", (str, problem) -> str + "\n\t" + problem.getVerboseMessage(), (str1, str2) -> str1 + str2);
            var storage = parseResult.getResult().flatMap(CompilationUnit::getStorage);
            if (storage.isPresent()) {
                LOGGER.warn("Problem(s) in parse result for file {}:{}", storage.get().getFileName(), problemString);
            } else {
                LOGGER.warn("Problem(s) in unknown parse result:{}", problemString);
            }
        }

        return parseResult;
    }
}
