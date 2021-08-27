package me.soels.thesis.repositories;

import me.soels.thesis.model.DataRelationship;
import me.soels.thesis.model.DependenceRelationship;
import me.soels.thesis.model.OtherClass;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.UUID;

public interface OtherClassRepository extends ClassRepository<OtherClass> {
    /**
     * Adds a data dependency relationship between the two given nodes with the given relationship properties.
     * <p>
     * See {@link ClassRepository#addDependencyRelationship(UUID, UUID, DependenceRelationship)} for more information
     * on this query.
     *
     * @param callerId     the id of the caller, the start node
     * @param calleeId     the id of the callee, the end node
     * @param relationship the relationship containing the properties to set
     */
    @Query("MATCH (a:OtherClass) " +
            "WITH a " +
            "MATCH (b:DataClass) " +
            "WITH a, b " +
            "WHERE a.id = $0 AND b.id = $1 " +
            "CREATE (a)-[r:DataDepends]->(b) " +
            "SET r = $2.__properties__")
    void addDataRelationship(UUID callerId, UUID calleeId, DataRelationship relationship);
}
