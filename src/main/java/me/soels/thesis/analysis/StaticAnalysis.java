package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;
import me.soels.thesis.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;
import static me.soels.thesis.analysis.ZipExtractor.extractZip;

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
        var dataClasses = identifyDataClasses(allTypes);
        var otherClasses = allTypes.stream()
                .map(clazz -> new OtherClass(clazz.getFullyQualifiedName().get(), clazz.getNameAsString()))
                .filter(clazz -> dataClasses.stream().noneMatch(dataClazz -> dataClazz.getIdentifier().equals(clazz.getIdentifier())))
                .collect(Collectors.toList());
        var classDependencies = resolveClassDependencies();
        var dataRelationships = resolveDataRelationship();

        System.out.println("Static analysis took " + (System.currentTimeMillis() - start) + " ms");

        modelBuilder.withDataClasses(dataClasses)
                .withOtherClasses(otherClasses)
                .withDependencies(classDependencies)
                .withDataRelationships(dataRelationships);
    }

    private List<ClassOrInterfaceDeclaration> getAllTypes(Path projectLocation, ParserConfiguration.LanguageLevel languageLevel) {
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
                // Also include the inner types     TODO: This now excludes enums, do we want those?
                .flatMap(type -> type.findAll(ClassOrInterfaceDeclaration.class).stream())
                // Print problems where FQN could not be determined and filter those cases out
                .peek(this::printEmptyQualifiers)
                .filter(clazz -> clazz.getFullyQualifiedName().isPresent())
                .collect(Collectors.toList());
    }

    private List<ParseResult<CompilationUnit>> parseRoot(SourceRoot root) {
        try {
            return root.tryToParse();
        } catch (IOException e) {
            throw new IllegalStateException("Could not process one of the file in source root " + root.getRoot(), e);
        }
    }

    private List<DependenceRelationship> resolveClassDependencies() {
        // TODO: Implement retrieval of class to class dependencies
        return new ArrayList<>();
    }

    private List<DataClass> identifyDataClasses(List<ClassOrInterfaceDeclaration> allTypes) {
        // TODO: Implement identification of data classes
        return new ArrayList<>();
    }

    private List<DataRelationship> resolveDataRelationship() {
        // TODO: Implement retrieval of class to data relationships
        return new ArrayList<>();
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