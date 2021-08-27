package me.soels.thesis.model;

import lombok.Getter;
import lombok.Setter;
import me.soels.thesis.solver.moea.encoding.EncodingType;
import org.springframework.data.neo4j.core.schema.Node;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Optional;

/**
 * Models the configuration of an evaluation.
 * <p>
 * The configuration contains all the information necessary to, given the input for the objectives, perform the
 * multi-objective clustering. This therefore primarily configures the problem statement of the evaluation.
 */
@Node
@Getter
@Setter
public class HierarchicalConfiguration extends SolverConfiguration {

}

