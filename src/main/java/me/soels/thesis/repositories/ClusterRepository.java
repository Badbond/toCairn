package me.soels.thesis.repositories;

import me.soels.thesis.model.Cluster;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface ClusterRepository extends Neo4jRepository<Cluster, UUID> {
}
