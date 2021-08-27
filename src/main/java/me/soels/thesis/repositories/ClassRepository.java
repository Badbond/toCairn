package me.soels.thesis.repositories;

import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.DependenceRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.UUID;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, UUID> {
    /**
     * Adds a dependency relationship between the two given nodes with the given relationship properties.
     * <p>
     * Note that we use {@code MATCH} together with {@code WITH} to force index lookups instead of having
     * a cartesian production comparison. See <a href="https://stackoverflow.com/a/33354771">SO comment</a>.
     * <p>
     * Note that we use {@code SET} to additionally set the properties on the relationship. See
     * <a href="https://neo4j.com/docs/cypher-manual/current/clauses/set/#set-set-all-properties-using-a-parameter">
     * Cypher documentation</a>
     * <p>
     * This method is partially derived from
     * <a href="https://community.neo4j.com/t/super-frustrated-sdn-deleting-existing-relationships/35245/18">
     * a Neo4J community post</a> about storing relationships.
     *
     * @param callerId     the id of the caller, the start node
     * @param calleeId     the id of the callee, the end node
     * @param relationship the relationship containing the properties to set
     */
    @Query("MATCH (a:AbstractClass) " +
            "WITH a " +
            "MATCH (b:AbstractClass) " +
            "WITH a, b " +
            "WHERE a.id = $0 AND b.id = $1 " +
            "CREATE (a)-[r:InteractsWith]->(b) " +
            "SET r = $2.__properties__")
    void addDependencyRelationship(UUID callerId, UUID calleeId, DependenceRelationship relationship);
}
