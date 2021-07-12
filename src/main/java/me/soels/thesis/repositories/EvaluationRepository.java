package me.soels.thesis.repositories;

import me.soels.thesis.model.Evaluation;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationRepository extends Neo4jRepository<Evaluation, UUID> {
    @Query("MATCH (eval :Evaluation)-[c]-(config :EvaluationConfiguration) " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result)")
    List<Evaluation> findAllShallow();

    @Query("MATCH (eval :Evaluation)-[c]-(config :EvaluationConfiguration) " +
            "WITH eval, c, config " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "WHERE eval.id = $0 " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result) " +
            "LIMIT 1")
    Optional<Evaluation> getByIdShallow(UUID evaluationId);
}
