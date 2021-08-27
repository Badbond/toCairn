package me.soels.thesis.repositories;

import me.soels.thesis.model.SolverConfiguration;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface EvaluationConfigurationRepository extends Neo4jRepository<SolverConfiguration, UUID> {
}
