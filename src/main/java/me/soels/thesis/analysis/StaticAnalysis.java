package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.thesis.analysis.ZipExtractor.extractZip;
import static me.soels.thesis.model.DataRelationshipType.READ;
import static me.soels.thesis.model.DataRelationshipType.WRITE;

/**
 * Performs static analysis of the given {@code .zip} file containing the application's source files.
 * <p>
 * With static analysis, we build the model that contains the data classes identified in the application, the other
 * classes containing business logic, relationships between these other classes and relationships between other classes
 * and data classes.
 * <p>
 * We use {@link JavaParser} for building the abstract syntax tree and resolving the type references. Some of this class
 * is based on their book <i>'JavaParser: Visited'</i>.
 */
public class StaticAnalysis {
    // TODO: Would rather have injection set up to inject the service instead.
    private final MethodCallDeclaringClassResolver declaringClassResolver = new MethodCallDeclaringClassResolver();
    private int count = 0;

    // TODO: Split in class analysis and relationship analysis.
    public void analyze(AnalysisModelBuilder modelBuilder, StaticAnalysisInput input) {
        System.out.println("Starting static analysis on " + input.getPathToZip());

        var inputZip = input.getPathToZip();
        if (!inputZip.getFileName().toString().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("The path does not refer to a .zip file, for path " + inputZip);
        } else if (!Files.exists(inputZip)) {
            throw new IllegalArgumentException("The zip file does not exist for path " + inputZip);
        }

        var projectLocation = extractZip(inputZip);
        performAnalysis(modelBuilder, projectLocation, input.getLanguageLevel());
    }

    private void performAnalysis(AnalysisModelBuilder modelBuilder, Path projectLocation, ParserConfiguration.LanguageLevel languageLevel) {
        var start = System.currentTimeMillis();

        System.out.println("Extracting classes");
        var allTypes = getAllTypes(projectLocation, languageLevel);
        var allClasses = allTypes.stream().map(Pair::getKey).collect(Collectors.toList());
        var allMethodNames = allTypes.stream()
                .flatMap(pair -> pair.getValue().findAll(MethodDeclaration.class).stream())
                .map(NodeWithSimpleName::getNameAsString)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        var otherClasses = filterClasses(allTypes, OtherClass.class);
        var dataClasses = filterClasses(allTypes, DataClass.class);
        System.out.printf("Graph nodes results:" +
                "%n    Total classes:             " + allClasses.size() +
                "%n    Data classes:              " + dataClasses.size() +
                "%n    Other classes:             " + otherClasses.size() +
                "%n    Total method declarations: " + allMethodNames.size() + "%n");

        System.out.println("Extracting relationships");
        var allRelationships = allTypes.stream()
                // Only calculate relationships from service classes
                .filter(pair -> pair.getKey() instanceof OtherClass)
                .flatMap(pair -> this.resolveClassDependencies((OtherClass) pair.getKey(), allClasses, allMethodNames, pair.getValue()).stream())
                .collect(Collectors.toList());

        var dataRelationships = filterRelationships(allRelationships, DataRelationship.class);
        var classDependencies = filterRelationships(allRelationships, DependenceRelationship.class);

        System.out.printf("Graph edges results:" +
                "%n    Total matching method calls:   " + declaringClassResolver.getTotalCount() +
                "%n    Unresolved calls:              " + declaringClassResolver.getErrorCount() +
                "%n    Resolved calls:                " + declaringClassResolver.getIdentifiedCount() +
                "%n    To classes within application: " + declaringClassResolver.getCalleeCount() +
                "%n    Excluding self-reference:      " + count +
                "%n    Total relationships:           " + allRelationships.size() +
                "%n    Data relationships:            " + dataRelationships.size() +
                "%n    Other class dependencies:      " + classDependencies.size() +
                "%n");
        System.out.println("Static analysis took " + (System.currentTimeMillis() - start) + " ms");

        modelBuilder.withDataClasses(dataClasses)
                .withOtherClasses(otherClasses)
                .withDependencies(classDependencies)
                .withDataRelationships(dataRelationships);
    }

    private List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> getAllTypes(Path projectLocation, ParserConfiguration.LanguageLevel languageLevel) {
        var config = new ParserConfiguration().setLanguageLevel(languageLevel);
        return new SymbolSolverCollectionStrategy(config)
                .collect(projectLocation).getSourceRoots().stream()
                // Don't include test directories
                .filter(root -> !root.getRoot().toString().contains("/test/"))
                // Parse the source roots, resolving the class types. We do not parallelize as that gave errors sometimes.
                .flatMap(root -> parseRoot(root).stream())
                // Print problems, filter those resulting in no parse result, and retrieve the types defined in the result
                .peek(this::printProblems)
                .filter(parseResult -> parseResult.getResult().isPresent())
                .flatMap(parseResult -> parseResult.getResult().get().getTypes().stream())
                // Also include the inner types
                // TODO: This now excludes enums and annotations, do we want those?
                .flatMap(type -> type.findAll(ClassOrInterfaceDeclaration.class).stream())
                // Print problems where FQN could not be determined and filter those cases out
                .peek(this::printEmptyQualifiers)
                .filter(clazz -> clazz.getFullyQualifiedName().isPresent())
                // Create a pair of the type of class and its AST
                .map(clazz -> Pair.of(identifyClass(clazz), clazz))
                .collect(Collectors.toList());
    }

    private AbstractClass identifyClass(ClassOrInterfaceDeclaration clazz) {
        var fqn = clazz.getFullyQualifiedName()
                .orElseThrow(() -> new IllegalStateException("Could not retrieve FQN from already filtered class"));

        if (isDataClass(clazz)) {
            return new DataClass(fqn, clazz.getNameAsString(), null);
        } else {
            return new OtherClass(fqn, clazz.getNameAsString());
        }
    }

    private boolean isDataClass(ClassOrInterfaceDeclaration clazz) {
        // TODO: check if class is a data class based on class characteristics
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

    private void printEmptyQualifiers(TypeDeclaration<?> typeDeclaration) {
        if (typeDeclaration.getFullyQualifiedName().isEmpty()) {
            // This usually happens with inner types, but as we do symbol solving, it should not happen.
            // In any case, we will exclude these from the analysis as we can not uniquely identify them in the graph.
            System.out.println("Could not construct FQN for type " + typeDeclaration.getNameAsString() + ". Skipping it.");
        }
    }

    private void printProblems(ParseResult<CompilationUnit> parseResult) {
        if (parseResult.isSuccessful()) {
            return;
        }

        if (parseResult.getResult().isPresent() && parseResult.getResult().get().getStorage().isPresent()) {
            System.out.printf("Problem(s) in parse result %s%n", parseResult.getResult().get().getStorage().get().getFileName());
        } else {
            System.out.println("Problem(s) in unknown parse result");
        }
        parseResult.getProblems().forEach(problem -> System.out.printf("        %s%n", problem.getVerboseMessage()));
    }
}
