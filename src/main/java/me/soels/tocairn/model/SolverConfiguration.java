package me.soels.tocairn.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.tocairn.solver.Solver;
import me.soels.tocairn.solver.metric.MetricType;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Models the configuration of a {@link Solver}.
 */
@Node
@Getter
@Setter
public abstract class SolverConfiguration {
    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private UUID id;

    @Nullable
    @Size(min = 2)
    private Integer minClusterAmount;
    @Nullable
    @Size(min = 2)
    private Integer maxClusterAmount;
    private List<MetricType> metrics = new ArrayList<>();

    public Optional<Integer> getMinClusterAmount() {
        return Optional.ofNullable(minClusterAmount);
    }

    public Optional<Integer> getMaxClusterAmount() {
        return Optional.ofNullable(maxClusterAmount);
    }
}
