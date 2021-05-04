package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Node
@Getter
@Setter
public class Microservice {
    private final int microserviceNumber;
    @Relationship("HasClasses")
    private Set<OtherClass> classes = new HashSet<>();
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    public Microservice(int microserviceNumber, Set<OtherClass> classes) {
        this.microserviceNumber = microserviceNumber;
        this.classes.addAll(classes);
    }
}
