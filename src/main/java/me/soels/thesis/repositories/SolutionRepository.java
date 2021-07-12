package me.soels.thesis.repositories;

import me.soels.thesis.model.Solution;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;
import java.util.UUID;

public interface SolutionRepository extends Neo4jRepository<Solution, UUID> {
    @Query("MATCH (solution :Solution)-[c]-(cluster :Cluster) " +
            "WITH solution, c, cluster " +
            "MATCH (cluster)-[i]-(class :AbstractClass) " +
            "WHERE solution.id = $0 " +
            "RETURN solution, collect(c), collect(cluster), collect(i), collect(class) " +
            "LIMIT 1")
    Optional<Solution> getByIdShallow(UUID solutionId);
}
