package me.soels.thesis.model;

public enum AnalysisStatus {
    /**
     * The analysis data is being set up and is still missing information.
     * This can happen when not all the required inputs have been delivered for the measurements configured.
     */
    INCOMPLETE,

    /**
     * All the input data is given and the analysis is configured. We are awaiting running the analysis itself.
     */
    PENDING,

    /**
     * The analysis is running
     */
    RUNNING,

    /**
     * The analysis errored
     */
    ERRORED,

    /**
     * The analysis is done and the solutions are stored.
     */
    DONE
}
