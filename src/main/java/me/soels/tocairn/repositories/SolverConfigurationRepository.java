package me.soels.tocairn.repositories;

import me.soels.tocairn.model.SolverConfiguration;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface SolverConfigurationRepository extends Neo4jRepository<SolverConfiguration, UUID> {
}
