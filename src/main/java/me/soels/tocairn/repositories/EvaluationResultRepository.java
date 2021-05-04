package me.soels.tocairn.repositories;

import me.soels.tocairn.model.EvaluationResult;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationResultRepository extends Neo4jRepository<EvaluationResult, UUID> {
    @Query("MATCH (result :EvaluationResult)-[r1]-(solution :Solution) " +
            "WHERE result.id = $0 " +
            "RETURN result, collect(r1), collect(solution) " +
            "LIMIT 1")
    Optional<EvaluationResult> getByIdShallow(UUID evaluationResultId);

    @Query("MATCH (result :EvaluationResult)-[r1]-(solution :Solution)-[r2]-(microservice :Microservice) " +
            "WHERE result.id = $0 " +
            "RETURN result, collect(r1), collect(solution), collect(r2), collect(microservice) " +
            "LIMIT 1")
    Optional<EvaluationResult> getByIdShallowWithMicroservices(UUID evaluationResultId);


    /**
     * Adds a relationship between the result and its solutions.
     * <p>
     * Note that we use {@code MATCH} together with {@code WITH} to force index lookups instead of having
     * a cartesian production comparison. See <a href="https://stackoverflow.com/a/33354771">SO comment</a>.
     * <p>
     * This method is partially derived from
     * <a href="https://community.neo4j.com/t/super-frustrated-sdn-deleting-existing-relationships/35245/18">
     * a Neo4J community post</a> about storing relationships.
     *
     * @param resultId    the id of the result
     * @param solutionIds the ids of the solutions in the result
     */
    @Query("MATCH (r:EvaluationResult) " +
            "WHERE r.id = $0 " +
            "WITH r " +
            "MATCH (s:Solution) " +
            "WHERE s.id IN $1 " +
            "CREATE (r)-[x:HasSolution]->(s)")
    void createSolutionRelationship(UUID resultId, List<UUID> solutionIds);
}
