package me.soels.tocairn.solver.moeca;

import lombok.Getter;
import me.soels.tocairn.solver.SolverFactory;
import org.moeaframework.Executor;

/**
 * Custom {@link Executor} for our {@link MOECASolver}.
 * <p>
 * We need to retrieve the {@link MOECAProblem} instance outside of the {@link SolverFactory} to normalize the metrics
 * seen after running the solver.
 */
@Getter
public class MOECAExecutor extends Executor {
    private final MOECAProblem problem;

    public MOECAExecutor(MOECAProblem problem) {
        super();
        this.problem = problem;
        withProblem(problem);
    }
}
