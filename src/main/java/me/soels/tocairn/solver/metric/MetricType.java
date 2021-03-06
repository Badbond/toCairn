package me.soels.tocairn.solver.metric;

import me.soels.tocairn.model.SolverConfiguration;
import me.soels.tocairn.solver.Solver;

import java.util.List;

/**
 * Enum used to identify and configure metrics used in a {@link Solver} through its {@link SolverConfiguration}.
 * <p>
 * Exposes a {@link #getMetrics()} function to retrieve the metrics for this type. We can not model this in the enum
 * as it will be stored in the graph database.
 */
public enum MetricType {
    DATA_AUTONOMY,
    FOCUSED_ON_ONE,
    BEHAVIORAL_AUTONOMY,
    DECOUPLED_COHESIVE,
    REUSABLE,
    LIMITED_COMMUNICATION_OVERHEAD,
    MODULARISED_FEATURES;

    public List<Metric> getMetrics() {
        switch (this) {
            case DATA_AUTONOMY:
                return List.of(new SelmadjiFIntra(), new SelmadjiFInter());
            case FOCUSED_ON_ONE:
                return List.of(new SelmadjiFOne());
            case BEHAVIORAL_AUTONOMY:
                return List.of(new SelmadjiFAutonomy());
            case DECOUPLED_COHESIVE:
                return List.of(new CarvalhoCohesion(), new CarvalhoCoupling());
            case REUSABLE:
                return List.of(new CarvalhoReusable());
            case LIMITED_COMMUNICATION_OVERHEAD:
                return List.of(new CarvalhoOverhead());
            case MODULARISED_FEATURES:
                return List.of(new CarvalhoFeatureModularization());
            default:
                throw new IllegalStateException("Unknown metric type " + this);
        }
    }
}
