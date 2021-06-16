package me.soels.thesis.repositories;

import me.soels.thesis.model.Solution;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface SolutionRepository extends Neo4jRepository<Solution, UUID> {
}
