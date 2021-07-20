package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Node
@Getter
@Setter
public class Cluster {
    private final int clusterNumber;
    @Relationship("HAS_NODES")
    private final List<AbstractClass> nodes = new ArrayList<>();
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    public Cluster(int clusterNumber, List<? extends AbstractClass> nodes) {
        this.clusterNumber = clusterNumber;
        this.nodes.addAll(nodes);
    }
}
