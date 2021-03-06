package me.soels.tocairn.repositories;

import me.soels.tocairn.model.Evaluation;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationRepository extends Neo4jRepository<Evaluation, UUID> {
    @Query("MATCH (eval :Evaluation)-[c]-(config :SolverConfiguration) " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result)")
    List<Evaluation> findAllShallow();

    @Query("MATCH (eval :Evaluation)-[c]-(config :SolverConfiguration) " +
            "WHERE eval.id = $0 " +
            "WITH eval, c, config " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result) " +
            "LIMIT 1")
    Optional<Evaluation> getByIdShallow(UUID evaluationId);

    /**
     * Updates the evaluation for the given {@code id} in a shallow manner.
     * <p>
     * This only allows to update specific property fields. It is needed as otherwise Neo4J will try to match the Java
     * model (containing all recursive relationships, i.e. the whole graph) with that stored in the database. This
     * behavior is not trusted due to recursion taking endlessly or due to the OGM breaking down relationships
     * prematurely. Using interface or class projections did not fix this either. Manual querying to override all
     * properties was not possible either due to the use of composite properties which require special handling by the
     * querying mechanism.
     * <p>
     * Note that, as we use hierarchies in configurations, we only persist property values for the evaluation object
     * and will not alter the state of any other resource. This needs to be done using their respective repositories.
     * <p>
     * This method is preferred over {@link #save(Object)} except during evaluation creation.
     *
     * @param evaluation the evaluation to shallowly persist
     * @return the updated shallow evaluation
     */
    @Query("MATCH (eval: Evaluation) " +
            "WHERE eval.id = $0.__id__ " +
            "SET eval += { name: $0.__properties__.name, status: $0.__properties__.status, executedAnalysis: $0.__properties__.executedAnalysis } " +
            "WITH eval " +
            "OPTIONAL MATCH (eval: Evaluation)-[c]-(config :SolverConfiguration) " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "RETURN eval, collect(c), collect(config), collect(r), collect(result)")
    Evaluation saveShallow(Evaluation evaluation);

    /**
     * Adds a relationship between the evaluation and one result.
     * <p>
     * Note that we use {@code MATCH} together with {@code WITH} to force index lookups instead of having
     * a cartesian production comparison. See <a href="https://stackoverflow.com/a/33354771">SO comment</a>.
     * <p>
     * This method is partially derived from
     * <a href="https://community.neo4j.com/t/super-frustrated-sdn-deleting-existing-relationships/35245/18">
     * a Neo4J community post</a> about storing relationships.
     *
     * @param evaluationId the id of the evaluation
     * @param resultId     the id of the evaluation result
     */
    @Query("MATCH (a :Evaluation) " +
            "WHERE a.id = $0 " +
            "WITH a " +
            "MATCH (b:EvaluationResult) " +
            "WHERE b.id = $1 " +
            "CREATE (a)-[:HasResult]->(b)")
    void createResultRelationship(UUID evaluationId, UUID resultId);

    /**
     * Performs a cascaded delete for an evaluation.
     *
     * @param evaluationId the evaluation to delete
     */
    @Query("MATCH (e :Evaluation) " +
            "WHERE e.id = $0 " +
            "WITH e " +
            "OPTIONAL MATCH (e)--(conf1 :AHCAConfiguration) " +
            "OPTIONAL MATCH (e)--(conf2 :MOECAConfiguration) " +
            "OPTIONAL MATCH (e)--(r :EvaluationResult) " +
            "OPTIONAL MATCH (e)--(c :AbstractClass) " +
            "OPTIONAL MATCH (r)--(s :Solution) " +
            "OPTIONAL MATCH (s)--(ms :Microservice) " +
            "DETACH DELETE e, conf1, conf2, r, c, s, ms")
    void cascadeDelete(UUID evaluationId);

    /**
     * Tries to find the evaluation with the given ID and returns the full entity including the classes input graph.
     *
     * @param id the id of the evaluation to retrieve
     * @return the evaluation with classes or empty if no evaluation was found
     * @see #getByIdShallow(UUID)
     */
    @Query("MATCH (e :Evaluation) " +
            "WHERE e.id = $0 " +
            "WITH e " +
            "MATCH (e)-[r1]-(conf :SolverConfiguration) " +
            "OPTIONAL MATCH (e)-[r2]-(r :EvaluationResult) " +
            "OPTIONAL MATCH (e)-[r3]-(c :AbstractClass) " +
            "OPTIONAL MATCH (r)-[r4]-(s :Solution) " +
            "OPTIONAL MATCH (s)-[r5]-(ms :Microservice) " +
            "OPTIONAL MATCH (c)-[r6]-(:AbstractClass) " +
            "RETURN e, conf, collect(r), collect(c), collect(s), collect(ms), " +
            "collect(r1), collect(r2), collect(r3), collect(r4), collect(r5), collect(r6)")
    Optional<Evaluation> findByIdDeep(UUID id);
}
