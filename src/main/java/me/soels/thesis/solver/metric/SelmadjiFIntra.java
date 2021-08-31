package me.soels.thesis.solver.metric;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FIntra metric as proposed by Selmadji et al. (2020).
 * <p>
 * This metric should be maximized but, as the MOEA framework only allows for minimization objectives, we negate the
 * value.
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFIntra extends SelmadjiDataAutonomy {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return -1 * clustering.getByCluster().values().stream()
                .mapToDouble(this::fintra)
                .sum();
    }

    private double fintra(List<OtherClass> classes) {
        // Get the cardinality of the set of data classes manipulated by classes in this microservice
        var nbDataManipulatedInMicro = nbDataManipulatedInMicro(classes);

        // Get all the pairs of classes in this microservice excluding self-pairs.
        var pairs = classes.stream()
                .flatMap(i -> classes.stream()
                        .filter(j -> !i.equals(j))
                        .map(j -> Pair.of(i, j)))
                .collect(Collectors.toList());

        if (pairs.isEmpty()) {
            // We do not have any pairs in this microservice to calculate this metric with.
            // We set the value to 0.0 to favour microservices with at least two classes.
            return 0.0;
        }

        // Calculate the metric
        return pairs.stream()
                .mapToDouble(pair -> data(pair.getKey(), pair.getValue(), nbDataManipulatedInMicro))
                .sum() / pairs.size();
    }
}
