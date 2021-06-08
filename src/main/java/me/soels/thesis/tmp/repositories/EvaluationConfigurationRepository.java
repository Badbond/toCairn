package me.soels.thesis.tmp.repositories;

import me.soels.thesis.tmp.daos.EvaluationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvaluationConfigurationRepository extends JpaRepository<EvaluationConfiguration, UUID> {
    Optional<EvaluationConfiguration> findByEvaluationId(UUID id);
}
