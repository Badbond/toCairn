package me.soels.thesis.tmp.services;

import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import me.soels.thesis.tmp.repositories.EvaluationConfigurationRepository;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

/**
 * Service responsible for managing {@link EvaluationConfiguration}.
 */
@Service
public class EvaluationConfigurationService {
    private final EvaluationConfigurationRepository repository;

    public EvaluationConfigurationService(EvaluationConfigurationRepository repository) {
        this.repository = repository;
    }

    public EvaluationConfiguration createConfiguration(EvaluationConfiguration configuration) {
        validate(configuration);
        return repository.save(configuration);
    }

    /**
     * Validates the configuration.
     * <p>
     * Partial validation is done through javax validation.
     *
     * @param configuration the configuration to validate
     */
    public void validate(@Valid EvaluationConfiguration configuration) {
        var boundViolation = configuration.getClusterCountLowerBound()
                .flatMap(lower -> configuration.getClusterCountUpperBound())
                .filter(upper -> upper < configuration.getClusterCountLowerBound().get());
        if (boundViolation.isPresent()) {
            throw new IllegalArgumentException("Cluster count upper bound needs to be greater than its lower bound");
        }
    }
}
