package me.soels.thesis.solver.metric;

import me.soels.thesis.model.SolverConfiguration;
import me.soels.thesis.solver.Solver;

import java.util.Set;

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
    SHARED_DEVELOPMENT_LIFECYCLE,
    DECOUPLED_COHESIVE,
    REUSABLE,
    LIMITED_COMMUNICATION_OVERHEAD,
    LIMITED_COMMUNICATION;

    public Set<Metric> getMetrics() {
        switch (this) {
            case DATA_AUTONOMY:
                return Set.of(new SelmadjiFIntra(), new SelmadjiFInter());
            case FOCUSED_ON_ONE:
                return Set.of(new SelmadjiFOne());
            case BEHAVIORAL_AUTONOMY:
                return Set.of(new SelmadjiFAutonomy());
            case SHARED_DEVELOPMENT_LIFECYCLE:
                return Set.of(new LohnertzEvolutionaryCouplingModularity());
            case DECOUPLED_COHESIVE:
                return Set.of(new CarvalhoCohesion(), new CarvalhoCoupling());
            case REUSABLE:
                return Set.of(new CarvalhoReusable());
            case LIMITED_COMMUNICATION_OVERHEAD:
                return Set.of(new CarvalhoOverhead());
            case LIMITED_COMMUNICATION:
                return Set.of(new LohnertzDynamicCoupling());
            default:
                throw new IllegalStateException("Unknown metric type " + this);
        }
    }
}
