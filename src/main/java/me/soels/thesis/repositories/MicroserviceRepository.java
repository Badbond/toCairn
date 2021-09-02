package me.soels.thesis.repositories;

import me.soels.thesis.model.Microservice;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.UUID;

public interface MicroserviceRepository extends Neo4jRepository<Microservice, UUID> {
    /**
     * Adds a relationship between the microservice and its class.
     * <p>
     * Note that we use {@code MATCH} together with {@code WITH} to force index lookups instead of having
     * a cartesian production comparison. See <a href="https://stackoverflow.com/a/33354771">SO comment</a>.
     * <p>
     * This method is partially derived from
     * <a href="https://community.neo4j.com/t/super-frustrated-sdn-deleting-existing-relationships/35245/18">
     * a Neo4J community post</a> about storing relationships.
     *
     * @param microserviceId the id of the microservice
     * @param classIds       the ids of the classes in the microservice
     */
    @Query("MATCH (a:Microservice) " +
            "WHERE a.id = $0 " +
            "WITH a " +
            "MATCH (b:AbstractClass) " +
            "WHERE b.id IN $1 " +
            "CREATE (a)-[r:HasClasses]->(b)")
    void createRelationships(UUID microserviceId, List<UUID> classIds);
}
