package me.soels.thesis.solver.objectives;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.solver.moea.encoding.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FIntra metric as proposed by Selmadji et al. (2020).
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class FIntra extends DataAutonomyMetric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return clustering.getByCluster().values().stream()
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

        // Calculate the metric
        return pairs.stream()
                .mapToDouble(pair -> data(pair.getKey(), pair.getValue(), nbDataManipulatedInMicro))
                .sum() / pairs.size();
    }
}
