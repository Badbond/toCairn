package me.soels.thesis.tmp.services;

import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

/**
 * Service responsible for managing {@link EvaluationConfiguration}.
 */
@Service
public class EvaluationConfigurationService {
    /**
     * Validates the configuration.
     * <p>
     * Partial validation is done through javax validation.
     *
     * @param configuration the configuration to validate
     */
    public void validate(@Valid EvaluationConfiguration configuration) {
        configuration.getClusterCountLowerBound()
                .flatMap(lower -> configuration.getClusterCountUpperBound())
                .filter(upper -> configuration.getClusterCountLowerBound().get() <= upper)
                .orElseThrow(() -> new IllegalArgumentException("Cluster count upper bound needs to be greater than " +
                        "its lower bound"));
    }
}
