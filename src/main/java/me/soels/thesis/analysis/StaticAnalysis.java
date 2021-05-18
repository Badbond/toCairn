package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static me.soels.thesis.analysis.ZipExtractor.extractZip;
import static me.soels.thesis.model.DataRelationshipType.READ;

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
    private int errorCount, count = 0;

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
        System.out.println("Found " + allClasses.size() + " classes of which " + dataClasses.size() +
                " data classes and " + otherClasses.size() + " other classes.");
        System.out.println("These classes contain " + allMethodNames.size() + " uniquely named methods");

        System.out.println("Extracting relationships");
        var allRelationships = allTypes.stream()
                // Only calculate relationships from service classes
                .filter(pair -> pair.getKey() instanceof OtherClass)
                .flatMap(pair -> this.resolveClassDependencies((OtherClass) pair.getKey(), allClasses, allMethodNames, pair.getValue()).stream())
                .collect(Collectors.toList());
        var dataRelationships = filterRelationships(allRelationships, DataRelationship.class);
        var classDependencies = filterRelationships(allRelationships, DependenceRelationship.class);
        System.out.println("Found " + allRelationships.size() + " relationships of which " + dataRelationships.size() +
                " data relationships and " + classDependencies + " other class dependencies");

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
                // Should not happen as we already filtered the stream on having FQN present
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

    // TODO: Remove debug logging
    private List<Relationship> resolveClassDependencies(OtherClass clazz, List<AbstractClass> allClasses, List<String> allMethodNames, ClassOrInterfaceDeclaration classDeclaration) {
        // TODO: In the process of seeing whether I need to control the way we traverse the tree for resolving the types
        //  This could perhaps be cleaned up based on what the outcome will be
        System.out.println("Checking class " + classDeclaration.getFullyQualifiedName().get());

        // We need to iterate this way to resolve statements in a preorder for multiple types of nodes
        var resolvedMethodCalls = new ArrayList<ResolvedMethodDeclaration>();
        classDeclaration.walk(Node.TreeTraversal.PREORDER, node -> {
            // TODO: Are method calls all the nodes that we want to include? Perhaps also field access or constructor calls
            if (MethodCallExpr.class.isAssignableFrom(node.getClass()) && allMethodNames.contains(((MethodCallExpr) node).getNameAsString())) {
                count++;
                // Resolve the method call to discover the type of the callee class
                if (classDeclaration.getNameAsString().equals("SomeClass")) {
                    System.out.println("Processing method " + ((MethodCallExpr) node).getNameAsString());
                }
                try {
                    resolvedMethodCalls.add(((MethodCallExpr) node).resolve());
                } catch (Exception e) {
                    errorCount++;
                }
            }
        });
        System.out.println("Processed " + count + " methods of which " + errorCount + " errored"); // 30403 - 18560

        return resolvedMethodCalls.stream()
                // Filter out targets that have not been observed (e.g. library classes, java internals).
                .filter(target -> allClasses.stream().anyMatch(c -> c.getIdentifier().equals(getClassFQN(target.getPackageName(), target.getClassName()))))
                // Group by callee class based on FQN
                .collect(groupingBy(method -> getClassFQN(method.getPackageName(), method.getClassName())))
                // TODO: Filter only on method calls to classes in application and filter out self.
                .values().stream()
                // For every callee class, identify the relationship
                .map(methodsCalledOfCallee -> identifyRelationship(clazz, methodsCalledOfCallee, allClasses))
                .collect(Collectors.toList());
    }

    private Relationship identifyRelationship(OtherClass clazz, List<ResolvedMethodDeclaration> targetMethods, List<AbstractClass> allClasses) {
        var fqn = getClassFQN(targetMethods.get(0).getPackageName(), targetMethods.get(0).getClassName());
        var target = allClasses.stream()
                .filter(otherClazz -> fqn.equals(otherClazz.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find class " + fqn + " in already parsed classes"));

        if (target instanceof DataClass) {
            return new DataRelationship(clazz, (DataClass) target, identifyReadWrite(targetMethods), targetMethods.size());
        } else if (target instanceof OtherClass) {
            return new DependenceRelationship(clazz, (OtherClass) target, targetMethods.size());
        } else {
            throw new IllegalStateException("Could not yet support class type " + target.getClass().getSimpleName());
        }
    }

    private DataRelationshipType identifyReadWrite(List<ResolvedMethodDeclaration> targetMethods) {
        // TODO: Based on getter/setter naming convention / parameter / return type identify the type of data relationship
        return READ;
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

    private String getClassFQN(String packageName, String className) {
        return packageName + "." + className;
    }
}
