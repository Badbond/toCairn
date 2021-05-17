package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.thesis.model.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;
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
    public static void main(String[] args) {
        var input = new StaticAnalysisInput(Path.of("/home/badbond/Downloads/thesis-project-master.zip"), JAVA_11);
        var analysis = new StaticAnalysis();
        var builder = new AnalysisModelBuilder();
        analysis.analyze(builder, input);
        builder.build();
    }

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

        var allTypes = getAllTypes(projectLocation, languageLevel);
        var allClasses = allTypes.stream().map(Pair::getKey).collect(Collectors.toList());

        var allRelationships = allTypes.stream()
                // Only calculate relationships from service classes
                .filter(pair -> pair.getKey() instanceof OtherClass)
                .flatMap(pair -> this.resolveClassDependencies((OtherClass) pair.getKey(), allClasses, pair.getValue()).stream())
                .collect(Collectors.toList());

        var otherClasses = filterClasses(allTypes, OtherClass.class);
        var dataClasses = filterClasses(allTypes, DataClass.class);
        var dataRelationships = filterRelationships(allRelationships, DataRelationship.class);
        var classDependencies = filterRelationships(allRelationships, DependenceRelationship.class);

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
                // Also include the inner types     TODO: This now excludes enums and annotations, do we want those?
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

    private List<Relationship> resolveClassDependencies(OtherClass clazz, List<AbstractClass> allClasses, ClassOrInterfaceDeclaration classDeclaration) {
        return classDeclaration.findAll(MethodCallExpr.class).stream()
                // Resolve the method call to discover the type of the callee class
                .map(MethodCallExpr::resolve)
                // Group by callee class based on FQN
                .collect(groupingBy(method -> method.getPackageName() + "." + method.getClassName()))
                // TODO: Filter only on method calls to classes in application and filter out self.
                .values().stream()
                // For every callee class, identify the relationship
                .map(methodsCalledOfCallee -> identifyRelationship(clazz, methodsCalledOfCallee, allClasses))
                .collect(Collectors.toList());
    }

    private Relationship identifyRelationship(OtherClass clazz, List<ResolvedMethodDeclaration> targetMethods, List<AbstractClass> allClasses) {
        var fqn = targetMethods.get(0).getPackageName() + "." + targetMethods.get(0).getClassName();
        var target = allClasses.stream()
                .filter(otherClazz -> fqn.equals(otherClazz.getIdentifier())).findFirst()
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

    @SuppressWarnings("unchecked") // We explicitly check the type with the provided class
    private <T extends AbstractClass> List<T> filterClasses(List<Pair<AbstractClass, ClassOrInterfaceDeclaration>> allClasses, Class<T> expectedClass) {
        return allClasses.stream()
                .filter(pair -> pair.getKey().getClass().isAssignableFrom(expectedClass))
                .map(pair -> (T) pair.getKey())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked") // We explicitly check the type with the provided class
    private <T extends Relationship> List<T> filterRelationships(List<Relationship> relationships, Class<T> expectedClass) {
        return relationships.stream()
                .filter(relationship -> relationship.getClass().isAssignableFrom(expectedClass))
                .map(relationship -> (T) relationship)
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
