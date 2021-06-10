package me.soels.thesis.tmp.repositories;

import me.soels.thesis.tmp.daos.AbstractClass;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, String> {
    // TODO: Remove/reimplement
//    @NonNull
//    List<T> findAllByEvaluationId(@NonNull UUID evaluationId);
}
