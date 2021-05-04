package me.soels.tocairn.solver;

import me.soels.tocairn.model.EvaluationResult;
import me.soels.tocairn.model.Solution;

import javax.annotation.Nullable;

/**
 * Represents an algorithm to solve the microservice identification problem.
 * <p>
 * As a solver holds state during execution, it should not be handled as a service but rather be initiated on every
 * execution.
 */
public interface Solver {
    /**
     * Returns the solutions produced by the solver based on the prepared state.
     *
     * @return the microservice boundaries identified
     */
    EvaluationResult run();

    /**
     * Initializes the solver with the optionally given solution.
     *
     * @param solution the solution to initialize the solver with
     * @param all      whether to have all solutions initialised with the given solution (for MOECA)
     */
    void initialize(@Nullable Solution solution, boolean all);
}
