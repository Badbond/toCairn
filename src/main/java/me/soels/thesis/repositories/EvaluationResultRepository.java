package me.soels.thesis.repositories;

import me.soels.thesis.model.EvaluationResult;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;
import java.util.UUID;

public interface EvaluationResultRepository extends Neo4jRepository<EvaluationResult, UUID> {
    @Query("MATCH (result :EvaluationResult)-[s]-(solution :Solution) " +
            "WHERE result.id = $0 " +
            "RETURN result, collect(s), collect(solution) " +
            "LIMIT 1")
    Optional<EvaluationResult> getByIdShallow(UUID evaluationResultId);
}
