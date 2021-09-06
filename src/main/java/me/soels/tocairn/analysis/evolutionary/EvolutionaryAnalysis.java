package me.soels.tocairn.analysis.evolutionary;

import me.soels.tocairn.model.EvaluationInputBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;

/**
 * Performs dynamic analysis of the given JFR file containing the application's runtime metrics.
 * <p>
 * With dynamic analysis, we enhance the evaluation input model with information that we extract from the runtime
 * metrics. This includes sizes of data classes and frequencies of classes invoking each other.
 */
@Service
public class EvolutionaryAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvolutionaryAnalysis.class);

    public void analyze(EvaluationInputBuilder modelBuilder, EvolutionaryAnalysisInput input) {
        LOGGER.info("Starting evolutionary analysis on {}", input.getPathToGitLog());
        var start = System.currentTimeMillis();

        var logFile = input.getPathToGitLog();
        if (!logFile.getFileName().toString().toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("The path does not refer to a .txt file, for path " + logFile);
        } else if (!Files.exists(logFile)) {
            throw new IllegalArgumentException("The git log file does not exist for path " + logFile);
        }

        // TODO: Perform evolutionary analysis:
        //  Either build myself or use Jakob's codebase somehow.
        //  Create relationships between classes indicating that they have been changed together in a commit.
        //  Think about property values that we want to set on this relationship:
        //      A frequency on how often they have been changed together?
        //      Some averaging heuristic on how often this class has been changed at all over the git history?
        //      Some averaging heuristic on how each commit based on the number of classes changed in the commit?
        //  Log the results from evolutionary analysis (what is the percentage of classes encountered?
        //      How many pairs? Average frequency? etc.
        var duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start);
        LOGGER.info("Evolutionary analysis took {} (H:m:s.millis)", duration);
    }
}
