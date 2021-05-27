package me.soels.thesis.analysis;

import com.github.javaparser.JavaParser;
import me.soels.thesis.model.AnalysisModelBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

import static me.soels.thesis.util.ZipExtractor.extractZip;

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
 */
public class StaticAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAnalysis.class);
    // TODO: Would rather have injection set up to inject the service instead.
    private final StaticClassAnalysis classAnalysis = new StaticClassAnalysis();
    private final StaticRelationshipAnalysis dependencyAnalysis = new StaticRelationshipAnalysis();

    public void analyze(AnalysisModelBuilder modelBuilder, StaticAnalysisInput input) {
        LOGGER.info("Starting static analysis on {}", input.getPathToZip());
        var start = System.currentTimeMillis();

        var inputZip = input.getPathToZip();
        if (!inputZip.getFileName().toString().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("The path does not refer to a .zip file, for path " + inputZip);
        } else if (!Files.exists(inputZip)) {
            throw new IllegalArgumentException("The zip file does not exist for path " + inputZip);
        }

        var projectLocation = extractZip(inputZip);
        var context = new StaticAnalysisContext(projectLocation, input);

        classAnalysis.analyze(context);
        dependencyAnalysis.analyze(context);

        context.applyResults(modelBuilder);

        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Total static analysis took {} (H:m:s.millis)", duration);
    }
}
