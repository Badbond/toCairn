package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import me.soels.thesis.model.AnalysisModelBuilder;
import me.soels.thesis.model.OtherClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;
import static me.soels.thesis.analysis.ZipExtractor.extractZip;

/**
 * Performs static analysis of the given {@code .zip} file containing the application's source files.
 * <p>
 * We use {@link JavaParser} for building the abstract syntax tree and resolving the type references. Some of this class
 * is based on their book <i>'JavaParser: Visited'</i>.
 */
public class StaticAnalysis {
    public static void main(String[] args) {
        var input = new StaticAnalysisInput(Path.of("/home/badbond/Downloads/thesis-project-develop.zip"), JAVA_11);
        var analysis = new StaticAnalysis();
        analysis.analyze(new AnalysisModelBuilder(), input);
    }

    public void analyze(AnalysisModelBuilder modelBuilder, StaticAnalysisInput input) {
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
        var config = new ParserConfiguration().setLanguageLevel(languageLevel);
        var types = new SymbolSolverCollectionStrategy(config)
                .collect(projectLocation).getSourceRoots().stream()
                .filter(root -> !root.getRoot().toString().contains("/test/")) // Don't include test directories
                .flatMap(root -> root.tryToParseParallelized().stream())
                .peek(this::printProblems)
                .filter(parseResult -> parseResult.getResult().isPresent())
                .flatMap(parseResult -> parseResult.getResult().get().getTypes().stream())
                .collect(Collectors.toList());

        // TODO: This is now all classes, not without the data classes.
        //  Also, the warning is valid and happens with inner classes. We need to solve that.
        modelBuilder.withOtherClasses(types.stream()
                .peek(this::printEmptyQualifiers)
                .filter(clazz -> clazz.getFullyQualifiedName().isPresent())
                .map(clazz -> new OtherClass(clazz.getFullyQualifiedName().get(), clazz.getNameAsString()))
                .collect(Collectors.toList())
        );

        System.out.println("Took " + (System.currentTimeMillis() - start) + " ms");
        // TODO: Symbol solving to get relationships between FQNs.
        var i = 0;
    }

    private void printEmptyQualifiers(TypeDeclaration<?> typeDeclaration) {
        if (typeDeclaration.getFullyQualifiedName().isEmpty()) {
            // This usually happens with inner types, but as we do symbol solving, it should not happen.
            // In any case, we will exclude these from the analysis as we can not uniquely identify them.
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

    private static class SomeInternalType {
        private final int something;

        private SomeInternalType(int something) {
            this.something = something;
        }

        public int getSomething() {
            return something;
        }
    }
}
