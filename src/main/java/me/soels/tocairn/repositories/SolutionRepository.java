package me.soels.tocairn.repositories;

import me.soels.tocairn.model.Solution;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolutionRepository extends Neo4jRepository<Solution, UUID> {
    @Query("MATCH (solution :Solution)-[c]-(microservice :Microservice) " +
            "WHERE solution.id = $0 " +
            "WITH solution, c, microservice " +
            "MATCH (microservice)-[i]-(class :AbstractClass) " +
            "RETURN solution, collect(c), collect(microservice), collect(i), collect(class) " +
            "LIMIT 1")
    Optional<Solution> getByIdShallow(UUID solutionId);

    /**
     * Adds a relationship between the solution and its microservices.
     * <p>
     * Note that we use {@code MATCH} together with {@code WITH} to force index lookups instead of having
     * a cartesian production comparison. See <a href="https://stackoverflow.com/a/33354771">SO comment</a>.
     * <p>
     * This method is partially derived from
     * <a href="https://community.neo4j.com/t/super-frustrated-sdn-deleting-existing-relationships/35245/18">
     * a Neo4J community post</a> about storing relationships.
     *
     * @param solutionId      the id of the solution
     * @param microserviceIds the ids of the microservices in the solution
     */
    @Query("MATCH (a:Solution) " +
            "WHERE a.id = $0 " +
            "WITH a " +
            "MATCH (b:Microservice) " +
            "WHERE b.id IN $1 " +
            "CREATE (a)-[r:HasMicroservice]->(b)")
    void createRelationships(UUID solutionId, List<UUID> microserviceIds);
}
