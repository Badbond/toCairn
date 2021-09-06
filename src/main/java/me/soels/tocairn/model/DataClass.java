package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

@Node
@Getter
@Setter
public final class DataClass extends AbstractClass {
    /**
     * Construct an instance of a data class.
     *
     * @param identifier        the identifier for this data class (FQN)
     * @param humanReadableName the human readable name for this data class
     * @param location          the location of the source file for this class
     */
    public DataClass(String identifier, String humanReadableName, String location) {
        super(identifier, humanReadableName, location);
    }
}
