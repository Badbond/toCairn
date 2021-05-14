package me.soels.thesis.analysis;

import java.util.List;

/**
 * The input for this application on what to analyze including the input data
 * to perform these analyses.
 */
public final class AnalysisInput {
    private final List<String> analysis;

    public AnalysisInput(List<String> analysis) {
        this.analysis = analysis;
    }

    public List<String> getAnalysis() {
        return analysis;
    }
}
