package me.soels.thesis.analysis.dynamic;

import me.soels.thesis.model.EvaluationInputBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;

/**
 * Performs dynamic analysis of the given JFR file containing the application's runtime metrics.
 * <p>
 * With dynamic analysis, we enhance the evaluation input model with information that we extract from the runtime
 * metrics. This includes sizes of data classes and frequencies of classes invoking each other.
 */
@Service
public class DynamicAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicAnalysis.class);

    @Async
    // TODO: Analysis is async, need a way of locking to prevent concurrent analysis or memory overflow.
    //  Also need a way to log or persist errors
    public void analyze(EvaluationInputBuilder modelBuilder, DynamicAnalysisInput input) {
        LOGGER.info("Starting dynamic analysis on {}", input.getPathToJfrLog());
        var start = System.currentTimeMillis();

        var logFile = input.getPathToJfrLog();
        if (!logFile.getFileName().toString().toLowerCase().endsWith(".jfr")) {
            throw new IllegalArgumentException("The path does not refer to a .jfr file, for path " + logFile);
        } else if (!Files.exists(logFile)) {
            throw new IllegalArgumentException("The log file does not exist for path " + logFile);
        }

        // TODO: Perform dynamic analysis:
        //  Set the average size for data classes stored in modelBuilder
        //  Set the exact amount of frequency of method calls for any class in modelBuilder. Classes that have not
        //      invoked each other during analysis should be ignored and remain the frequency from static analysis.
        //  Log the results from dynamic analysis (how many data classes have we measured size for?
        //      How many interactions have happened per relationship type? How many have we missed?)
        //  What do we want to do with data classes that have not been recorded? What will be default behavior?

        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Dynamic analysis took {} (H:m:s.millis)", duration);
    }
}
