package me.soels.thesis.repositories;

import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.DependenceRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.UUID;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, UUID> {
    @Query("MATCH (caller:AbstractClass), (callee:AbstractClass) " +
            "WHERE ID(caller) = $0.__id__ AND ID(callee) = $1.__id__ " +
            "CREATE (caller)-[r:DEPENDS_ON]->(callee) SET r = $2.__properties__")
    void addDependencyRelationship(T caller, T callee, DependenceRelationship relationship);
}
