package me.soels.thesis.solver;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.model.Solution;

import java.util.List;

/**
 * Represents an algorithm to solve the microservice identification problem.
 * <p>
 * As a solver holds state during execution, it should not be handled as a service but rather be initiated on every
 * execution.
 */
public interface Solver {
    /**
     * Returns the solutions produced by the solver based on the given input and prepared state.
     *
     * @param input the input to solve
     * @return the microservice boundaries identified
     */
    EvaluationResult run(EvaluationInput input);
}
