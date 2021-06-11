package me.soels.thesis.repositories;

import me.soels.thesis.model.Evaluation;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface EvaluationRepository extends Neo4jRepository<Evaluation, UUID> {
}
