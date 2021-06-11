package me.soels.thesis.repositories;

import me.soels.thesis.model.EvaluationResult;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface EvaluationResultRepository extends Neo4jRepository<EvaluationResult, UUID> {
}
