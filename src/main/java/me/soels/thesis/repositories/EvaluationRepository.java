package me.soels.thesis.repositories;

import me.soels.thesis.model.Evaluation;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

// TODO: Use projections to only retrieve what is needed. By default, everything is eagerly loaded -- which is a lot
//  in our case. See https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#projections
public interface EvaluationRepository extends Neo4jRepository<Evaluation, UUID> {
    Evaluation getById(UUID evaluationId);
}
