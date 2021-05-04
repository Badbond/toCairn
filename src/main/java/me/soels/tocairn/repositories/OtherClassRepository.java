package me.soels.tocairn.repositories;

import me.soels.tocairn.model.DataRelationship;
import me.soels.tocairn.model.DependenceRelationship;
import me.soels.tocairn.model.OtherClass;
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
            "WHERE a.id = $0 " +
            "WITH a " +
            "MATCH (b:DataClass) " +
            "WHERE b.id = $1 " +
            "CREATE (a)-[r:OperatesData]->(b) " +
            "SET r = $2.__properties__")
    void addDataRelationship(UUID callerId, UUID calleeId, DataRelationship relationship);
}
