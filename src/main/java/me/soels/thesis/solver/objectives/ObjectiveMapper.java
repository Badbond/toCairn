package me.soels.thesis.solver.objectives;

import java.util.List;

public class ObjectiveMapper {
    public static List<Metric> getMetricsForObjective(ObjectiveType objectiveType) {
        switch (objectiveType) {
            case ONE_PURPOSE:
                // TODO: We probably want to have only one metric per objective such that we can have one number
                //  indicate the quality of the solution based on that characteristic instead of having multiple.
                //  Perhaps we can sum these objectives. However, they should have equal ranges otherwise 0-10 would
                //  overtake 0-1 in almost all cases. Thus one_purpose=(cohesion+coupling)/2 should range from (0-1).
                return List.of(new CohesionCarvalhoMetric(), new CouplingCarvalhoMetric());
            case DATA_AUTONOMY:
                return List.of(new SelmadjiDataAutonomyMetric());
            case BOUNDED_CONTEXT:
                return List.of(new TemporaryBoundedContextMetric());
            case SHARED_DEVELOPMENT_LIFECYCLE:
                return List.of(new TemporarySharedLifecycleMetric());
            default:
                throw new IllegalStateException("Unsupported objective type found " + objectiveType);
        }
    }
}
