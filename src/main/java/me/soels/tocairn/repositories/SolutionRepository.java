package me.soels.tocairn.repositories;

import me.soels.tocairn.model.Solution;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;
import java.util.UUID;

public interface SolutionRepository extends Neo4jRepository<Solution, UUID> {
    @Query("MATCH (solution :Solution)-[c]-(microservice :Microservice) " +
            "WITH solution, c, microservice " +
            "MATCH (microservice)-[i]-(class :AbstractClass) " +
            "WHERE solution.id = $0 " +
            "RETURN solution, collect(c), collect(microservice), collect(i), collect(class) " +
            "LIMIT 1")
    Optional<Solution> getByIdShallow(UUID solutionId);
}
