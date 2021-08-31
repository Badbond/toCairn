package me.soels.thesis.repositories;

import me.soels.thesis.model.Evaluation;
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
            "WITH eval, c, config " +
            "OPTIONAL MATCH (eval: Evaluation)-[r]-(result :EvaluationResult) " +
            "WHERE eval.id = $0 " +
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
}
