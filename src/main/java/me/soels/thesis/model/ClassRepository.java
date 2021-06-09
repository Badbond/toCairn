package me.soels.thesis.model;

import me.soels.thesis.tmp.daos.AbstractClass;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, String> {
    @NonNull
    List<T> findAllByEvaluationId(@NonNull UUID evaluationId);
}
