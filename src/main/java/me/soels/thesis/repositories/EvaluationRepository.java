package me.soels.thesis.repositories;

import me.soels.thesis.model.Evaluation;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// TODO: Use projections to only retrieve what is needed. By default, everything is eagerly loaded -- which is a lot
//  in our case. See https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#projections
public interface EvaluationRepository extends Neo4jRepository<Evaluation, UUID> {

    @Query("MATCH (eval :Evaluation)-[c]-(config :EvaluationConfiguration) " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result)")
    List<Evaluation> findAllShallow();

    @Query("MATCH (eval :Evaluation)-[c]-(config :EvaluationConfiguration) " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "WHERE ID(eval) = $0 " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result) " +
            "LIMIT 1")
    Optional<Evaluation> getByIdShallow(UUID evaluationId);

    Evaluation getById(UUID evaluationId);
}
