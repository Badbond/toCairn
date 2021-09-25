package me.soels.tocairn.model;

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
public class Microservice {
    private final int microserviceNumber;
    @Relationship("HasClasses")
    private List<OtherClass> classes = new ArrayList<>();
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    public Microservice(int microserviceNumber, List<OtherClass> classes) {
        this.microserviceNumber = microserviceNumber;
        this.classes.addAll(classes);
    }
}
