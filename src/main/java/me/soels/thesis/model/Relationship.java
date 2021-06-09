package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import javax.persistence.Id;
import java.util.UUID;

/**
 * Indicates a relationships between two classes.
 *
 * @see DataRelationship
 * @see DependenceRelationship
 */
@Getter
@Setter
@RelationshipProperties
public abstract class Relationship {
    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private UUID id;
    @TargetNode
    private AbstractClass callee;

    protected Relationship(AbstractClass callee) {
        this.callee = callee;
    }
}
