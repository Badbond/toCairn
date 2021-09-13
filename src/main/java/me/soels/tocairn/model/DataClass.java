package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Set;

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
     * @param featureSet        the features this class implements
     */
    public DataClass(String identifier, String humanReadableName, String location, Set<String> featureSet) {
        super(identifier, humanReadableName, location, featureSet);
    }
}
