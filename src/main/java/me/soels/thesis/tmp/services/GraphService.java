package me.soels.thesis.tmp.services;

import me.soels.thesis.model.*;
import me.soels.thesis.tmp.daos.DataClass;
import me.soels.thesis.tmp.daos.OtherClass;
import me.soels.thesis.tmp.repositories.ClassRepository;
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
    private final ClassRepository<OtherClass> otherClassRepository;
    private final ClassRepository<DataClass> dataClassRepository;
    // TODO: Relationship repository

    public GraphService(@Qualifier("classRepository") ClassRepository<OtherClass> otherClassRepository,
                        @Qualifier("classRepository") ClassRepository<DataClass> dataClassRepository) {
        this.otherClassRepository = otherClassRepository;
        this.dataClassRepository = dataClassRepository;
    }

    public EvaluationInput getInput(UUID id) {
        return new EvaluationInputBuilder(id)
                .withClasses(dataClassRepository.findAllByEvaluationId(id))
                .withClasses(otherClassRepository.findAllByEvaluationId(id))
                // TODO: Add relationships
                .build();
    }

    public void storeInput(EvaluationInput input) {
        otherClassRepository.saveAll(input.getOtherClasses());
        dataClassRepository.saveAll(input.getDataClasses());
        // TODO: Store relationships
    }
}
