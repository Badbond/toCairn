package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;

@Node
@Getter
@Setter
public final class DataClass extends AbstractClass {
    private final Integer loc;
    private Integer measuredByteSize;

    /**
     * Construct an instance of a data class.
     *
     * @param identifier        the identifier for this data class (FQN)
     * @param humanReadableName the human readable name for this data class
     * @param loc               the size of the data class in bytes
     */
    public DataClass(String identifier, String humanReadableName, Integer loc) {
        super(identifier, humanReadableName);
        this.loc = loc;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), loc);
    }
}
