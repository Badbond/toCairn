package me.soels.tocairn.repositories;

import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.DependenceRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.UUID;

public interface ClassRepository<T extends AbstractClass> extends Neo4jRepository<T, UUID> {
    /**
     * Retrieves the input graph for the evaluation with the given ID.
     * <p>
     * The Neo4j ORM tries to resolve every relationship and becomes very inefficient when there are cycles in the
     * graph. For ~100 classes, ~80 solutions and ~3400 microservices, it tries to resolve cycles for the
     * 100 classes for every microservice. This is too much for Neo4j to handle. Therefore, we retrieve the graph
     * separately.
     *
     * @param evaluationId the evaluation to retrieve the classes for
     * @return the classes and their dependencies belonging to the given evaluation
     */
    List<T> findAllByEvaluationId(UUID evaluationId);

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
            "WHERE a.id = $0 " +
            "WITH a " +
            "MATCH (b:AbstractClass) " +
            "WHERE b.id = $1 " +
            "CREATE (a)-[r:InteractsWith]->(b) " +
            "SET r = $2.__properties__")
    void addDependencyRelationship(UUID callerId, UUID calleeId, DependenceRelationship relationship);

    /**
     * Sets the size of a class.
     * <p>
     * This circumvents model checking of the whole graph based on relationships when just storing the resource.
     *
     * @param classId the class to set the size for
     * @param size    the size to set
     */
    @Query("MATCH (a :AbstractClass) " +
            "WHERE a.id = $0 " +
            "SET a += { size: $1 }")
    void setSize(UUID classId, Long size);
}
