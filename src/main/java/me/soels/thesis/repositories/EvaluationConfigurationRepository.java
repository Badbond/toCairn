package me.soels.thesis.repositories;

import me.soels.thesis.model.EvaluationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvaluationConfigurationRepository extends JpaRepository<EvaluationConfiguration, UUID> {
    Optional<EvaluationConfiguration> findByEvaluationId(UUID id);
}
