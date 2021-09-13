package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.CompositeProperty;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Relationship between two {@link AbstractClass} that signify one depends on the other.
 * <p>
 * This relationship is used to identify non-data relationships in our graph. The {@link #getStaticFrequency()} is the number
 * of times that {@code caller} calls {@link #getCallee()} in a different location of the source code. Therefore,
 * this is constructed using static analysis. We don't use dynamic analysis as that is too costly to run for all
 * classes and methods. An exception to this is the {@link DataRelationship}.
 * <p>
 * This relationship also holds metadata for how often {@code caller} interacts with {@link #getCallee()}.
 * We keep track of static frequency by by the amount of unique method calls from {@code caller} to
 * {@link #getCallee()}. When dynamic data is provided, this will be determined based on
 * the amount of interactions in the time frame for which dynamic analysis was performed.
 */
@Getter
@Setter
public class DependenceRelationship extends Relationship {
    // Map showing how which classes (FQN) have been sent over for communication and how often
    @CompositeProperty(prefix = "sharedClasses")
    private Map<String, Long> sharedClasses = new HashMap<>();

    @NotNull
    private int staticFrequency;
    private Long dynamicFrequency;
    private long size;

    public DependenceRelationship(AbstractClass callee, int staticFrequency, Long dynamicFrequency, Map<String, Long> sharedClasses) {
        super(callee);
        this.staticFrequency = staticFrequency;
        this.dynamicFrequency = dynamicFrequency;
        this.sharedClasses.putAll(sharedClasses);
    }

    public Optional<Long> getDynamicFrequency() {
        // We could not store the relationship with optional properties and thus set it to -1 instead for not present
        return Optional.ofNullable(dynamicFrequency);
    }
}
