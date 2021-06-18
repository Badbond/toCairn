package me.soels.thesis.analysis.statik;

import com.github.javaparser.JavaParser;
import me.soels.thesis.model.EvaluationInputBuilder;
import me.soels.thesis.util.ZipExtractor;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;

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
@Service
public class StaticAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAnalysis.class);
    private final StaticClassAnalysis classAnalysis;
    private final StaticRelationshipAnalysis dependencyAnalysis;
    private final ZipExtractor zipExtractor;

    public StaticAnalysis(StaticClassAnalysis classAnalysis,
                          StaticRelationshipAnalysis dependencyAnalysis,
                          ZipExtractor zipExtractor) {
        this.classAnalysis = classAnalysis;
        this.dependencyAnalysis = dependencyAnalysis;
        this.zipExtractor = zipExtractor;
    }

    public void analyze(EvaluationInputBuilder modelBuilder, StaticAnalysisInput input) {
        LOGGER.info("Starting static analysis on {}", input.getPathToZip());
        var start = System.currentTimeMillis();

        var inputZip = input.getPathToZip();
        if (!inputZip.getFileName().toString().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("The path does not refer to a .zip file, for path " + inputZip);
        } else if (!Files.exists(inputZip)) {
            throw new IllegalArgumentException("The zip file does not exist for path " + inputZip);
        }

        var projectLocation = zipExtractor.extractZip(inputZip);
        var context = new StaticAnalysisContext(projectLocation, input, modelBuilder);

        classAnalysis.analyze(context);
        dependencyAnalysis.analyze(context);

        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Total static analysis took {} (H:m:s.millis)", duration);
    }
}
