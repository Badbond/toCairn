package me.soels.thesis.solver.metric;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.solver.Clustering;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FInter metric as proposed by Selmadji et al. (2020).
 * <p>
 * See related work 'Selmadji, A., Seriai, A. D., Bouziane, H. L., Mahamane, R. O., Zaragoza, P., & Dony, C. (2020,
 * March). From monolithic architecture style to microservice one based on a semi-automatic approach. In 2020 IEEE
 * International Conference on Software Architecture (ICSA) (pp. 157-168). IEEE.'.
 */
public class SelmadjiFInter extends SelmadjiDataAutonomy {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return clustering.getByCluster().values().stream()
                .mapToDouble(microservice -> finter(microservice, clustering))
                .sum();
    }

    private double finter(List<OtherClass> classes, Clustering clustering) {
        // Get the cardinality of the set of data classes manipulated by classes in this microservice
        var nbDataManipulatedInMicro = nbDataManipulatedInMicro(classes);

        if (nbDataManipulatedInMicro == 0) {
            // This class does not modify data. Therefore, it will not have
            return 0.0;
        }

        // Get all the classes that do not belong to this microservice.
        var externalClasses = clustering.getByClass().keySet().stream()
                .filter(clazz -> !classes.contains(clazz))
                .collect(Collectors.toList());

        if (externalClasses.isEmpty()) {
            // There are no more external classes, we have all classes in one microservice, i.e. monolith.
            // Set FInter to 0.
            return 0.0;
        }

        // Get all the pairs of classes in this microservice with those of external classes.
        var pairs = classes.stream()
                .flatMap(i -> externalClasses.stream()
                        .map(j -> Pair.of(i, j)))
                .collect(Collectors.toList());

        // Calculate the metric
        return pairs.stream()
                .mapToDouble(pair -> data(pair.getKey(), pair.getValue(), nbDataManipulatedInMicro))
                .sum() / pairs.size();
    }
}
