package me.soels.thesis.repositories;

import me.soels.thesis.model.AbstractClass;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, String> {
    // TODO: Remove/reimplement
//    @NonNull
//    List<T> findAllByEvaluationId(@NonNull UUID evaluationId);
}
