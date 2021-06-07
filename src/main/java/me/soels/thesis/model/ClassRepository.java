package me.soels.thesis.model;

import me.soels.thesis.model.AbstractClass;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, String> {
    @Override
    @NonNull
    Optional<T> findById(@NonNull String fqn);

    @Override
    @NonNull
    List<T> findAll();
}
