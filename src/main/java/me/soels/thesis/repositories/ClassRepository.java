package me.soels.thesis.repositories;

import me.soels.thesis.model.AbstractClass;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, UUID> {
}
