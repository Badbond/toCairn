package me.soels.thesis.analysis.sources;

import com.github.javaparser.JavaParser;
import me.soels.thesis.analysis.sources.jacoco.JacocoReportExtractor;
import me.soels.thesis.model.EvaluationInputBuilder;
import me.soels.thesis.util.ZipExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Performs source analysis of the given {@code .zip} file containing the application's source files.
 * <p>
 * With source analysis, we build the model that contains the data classes identified in the application, the other
 * classes containing business logic, relationships between these other classes and relationships between other classes
 * and data classes.
 * <p>
 * We use {@link JavaParser} for building the abstract syntax tree and resolving the type references. Some of this class
 * is based on their book <i>'JavaParser: Visited'</i>. Please note that we do not introduce any concurrency ourselves
 * on purpose as the library used returns more errors when doing so (both during parsing classes and resolving methods).
 * <p>
 * Note that this source analysis does not allow for resolving relations that are constructed at runtime. Therefore,
 * it does not allow to represent injection and polymorphism relations. We can mitigate this if desired using dynamic
 * analysis.
 * <p>
 * During this analysis, we don't only do static analysis but also incorporate data from dynamic analysis. For static
 * analysis we use the abstract syntax tree of the Java sources. For dynamic analysis (if provided), we use the
 * custom JaCoCo XMl report produced by <a href="https://github.com/Badbond/jacoco">our JaCoCo fork</a>. This will
 * then match the execution counts of source lines with those found in the abstract syntax tree.
 */
@Service
public class SourceAnalysis {
    private final SourceClassAnalysis classAnalysis;
    private final SourceRelationshipAnalysis dependencyAnalysis;
    private final ZipExtractor zipExtractor;
    private final JacocoReportExtractor reportExtractor;

    public SourceAnalysis(SourceClassAnalysis classAnalysis,
                          SourceRelationshipAnalysis dependencyAnalysis,
                          ZipExtractor zipExtractor,
                          JacocoReportExtractor reportExtractor) {
        this.classAnalysis = classAnalysis;
        this.dependencyAnalysis = dependencyAnalysis;
        this.zipExtractor = zipExtractor;
        this.reportExtractor = reportExtractor;
    }

    public SourceAnalysisContext prepareContext(EvaluationInputBuilder builder, SourceAnalysisInput input) throws IOException {
        var inputZip = input.getPathToZip();
        if (!inputZip.getFileName().toString().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("The path does not refer to a .zip file, for path " + inputZip);
        } else if (!Files.exists(inputZip)) {
            throw new IllegalArgumentException("The zip file does not exist for path " + inputZip);
        }
        var projectLocation = zipExtractor.extractZip(inputZip);
        var context = new SourceAnalysisContext(projectLocation, input, builder);

        if (input.getPathToJaCoCoXml().isPresent()) {
            var jacocoPath = input.getPathToJaCoCoXml().get();
            if (!jacocoPath.getFileName().toString().toLowerCase().endsWith(".xml")) {
                throw new IllegalArgumentException("The path does not refer to a .xml file, for path " + jacocoPath);
            } else if (!Files.exists(jacocoPath)) {
                throw new IllegalArgumentException("The XML file does not exist for path " + jacocoPath);
            }

            reportExtractor.extractJaCoCoReport(input.getPathToJaCoCoXml().get(), context.getSourceExecutions());
        }

        return context;
    }

    public void analyzeNodes(SourceAnalysisContext context) {
        classAnalysis.analyze(context);
    }

    public void analyzeEdges(SourceAnalysisContext context) {
        dependencyAnalysis.analyze(context);
    }
}
