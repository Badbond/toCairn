package me.soels.thesis.tmp.daos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

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
    @GeneratedValue
    private Long id;

    @TargetNode
    private AbstractClass callee;

    protected Relationship(AbstractClass callee) {
        this.callee = callee;
    }
}
