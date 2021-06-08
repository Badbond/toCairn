package me.soels.thesis.tmp.services;

import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.ClassRepository;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.EvaluationInputBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Manages the {@link EvaluationInput} input graph for the evaluation.
 * <p>
 * The graph is managed in a Neo4J database instead of a SQL database. Therefore, this service is responsible for
 * managing the bridge between the two databases. Neo4J does not support a notion of a graph such that multiple
 * graphs can be stored semantically separated in a database. Therefore, we store the ID of the evaluation in the
 * nodes and edges of the database such that the database is modeled as multiple disconnected graphs.
 */
@Service
public class GraphService {
    private final ClassRepository<AbstractClass> classRepository;
    // TODO: Relationship repository

    public GraphService(@Qualifier("classRepository") ClassRepository<AbstractClass> classRepository) {
        this.classRepository = classRepository;
    }

    public EvaluationInput getInputForEvaluation(UUID id) {
        var builder = new EvaluationInputBuilder();
        classRepository.findAllByEvaluationId(id);
        return builder.build();
    }
}
