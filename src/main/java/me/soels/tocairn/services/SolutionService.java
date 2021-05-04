package me.soels.tocairn.services;

import me.soels.tocairn.api.ResourceNotFoundException;
import me.soels.tocairn.api.dtos.CustomSolutionDto;
import me.soels.tocairn.model.*;
import me.soels.tocairn.repositories.ClassRepository;
import me.soels.tocairn.repositories.MicroserviceRepository;
import me.soels.tocairn.repositories.SolutionRepository;
import me.soels.tocairn.solver.ClusteringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static me.soels.tocairn.util.GenericCollectionExtractor.extractType;

/**
 * Service responsible for managing {@link EvaluationResult}
 */
@Service
public class SolutionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionService.class);
    private final EvaluationService evaluationService;
    private final SolutionRepository solutionRepository;
    private final MicroserviceRepository microserviceRepository;
    private final ClassRepository<AbstractClass> classRepository;

    public SolutionService(EvaluationService evaluationService,
                           SolutionRepository solutionRepository,
                           MicroserviceRepository microserviceRepository,
                           ClassRepository<AbstractClass> classRepository) {
        this.evaluationService = evaluationService;
        this.solutionRepository = solutionRepository;
        this.microserviceRepository = microserviceRepository;
        this.classRepository = classRepository;
    }

    /**
     * Returns the solution and its dependencies.
     *
     * @param solutionId the solution to retrieve
     * @return the solution and its dependencies
     * @throws ResourceNotFoundException when no solution could be found
     */
    public Solution getSolution(UUID solutionId) {
        return solutionRepository.findById(solutionId)
                .orElseThrow(() -> new ResourceNotFoundException(solutionId));
    }

    /**
     * Returns the solution without its dependencies.
     *
     * @param solutionId the solution to retrieve
     * @return the solution without its dependencies
     * @throws ResourceNotFoundException when no solution could be found
     */
    public Solution getShallowSolution(UUID solutionId) {
        return solutionRepository.getByIdShallow(solutionId)
                .orElseThrow(() -> new ResourceNotFoundException(solutionId));
    }

    /**
     * Persists the given solution and returns its persisted state with microservice relationships.
     * <p>
     * We need to remove the microservice relationships as well as the class relationships in the microservices to
     * perform the persistence queries. We then add them before returning again. Be wary of persisting this object
     * in any other service/repository.
     *
     * @param solution the solution to persist
     * @return the persisted solution with relationships
     */
    public Solution persistSolution(Solution solution) {
        LOGGER.info("Persisting solution");
        LOGGER.info(" - Temporarily storing microservices' classes and resetting them");
        Map<Microservice, Set<OtherClass>> microserviceClasses = new HashMap<>();
        for (var microservice : solution.getMicroservices()) {
            // Store the instance of this microservice and the classes it should reference.
            microserviceClasses.put(microservice, microservice.getClasses());
            // Remove the class references such that we can persist the microservice completely.
            microservice.setClasses(Collections.emptySet());
        }

        LOGGER.info(" - Persisting {} microservices.", microserviceClasses.size());
        microserviceClasses.keySet().parallelStream().forEach(microserviceRepository::save);

        LOGGER.info(" - Temporarily storing solution's microservices and resetting them");
        // Store the instance of this solution and the microservices it should reference.
        List<Microservice> microservices = solution.getMicroservices();
        // Remove the microservice references such that we can persist the result and solution completely.
        solution.setMicroservices(Collections.emptyList());

        LOGGER.info(" - Persisting solution.");
        solutionRepository.save(solution);

        LOGGER.info(" - Creating links from solutions to the microservices");
        solutionRepository.createRelationships(solution.getId(), microservices.stream()
                .map(Microservice::getId)
                .collect(Collectors.toList()));

        LOGGER.info(" - Creating links from microservices to the classes");
        microserviceClasses.forEach((microservice, classes) ->
                microserviceRepository.createRelationships(microservice.getId(), classes.stream()
                        .map(AbstractClass::getId)
                        .collect(Collectors.toList())
                )
        );

        LOGGER.info("Done persisting clustering solution {}", solution.getId());
        solution.setMicroservices(microservices);
        microservices.forEach(microservice -> microservice.setClasses(microserviceClasses.get(microservice)));
        return solution;
    }

    /**
     * Delete the solution with the given {@code id}.
     * <p>
     * Performs cascading delete for the microservices in this solution. The input graph is not deleted.
     *
     * @param solution the solution to delete
     */
    public void deleteSolution(Solution solution) {
        // Delete the clusters associated with the solution
        solution.getMicroservices().forEach(ms -> {
            // Reset classes to make sure the ORM does not delete them
            ms.setClasses(null);
            microserviceRepository.delete(ms);
        });

        // Delete the solution
        solutionRepository.delete(solution);
    }

    /**
     * Creates a custom solution from user input.
     * <p>
     * This does not persist the solution.
     *
     * @param evaluationId   the evaluation to create this solution under
     * @param customSolution the solution's definition
     * @return the solution and its dependencies
     */
    public Solution createCustomSolution(UUID evaluationId, CustomSolutionDto customSolution) {
        var microservices = new ArrayList<Microservice>();
        var inputGraph = classRepository.getInputNodesWithoutRel(evaluationId);
        var otherClasses = extractType(inputGraph, OtherClass.class).stream()
                .collect(Collectors.toMap(AbstractClass::getIdentifier, clazz -> clazz));
        var seen = new HashSet<String>();
        for (int i = 0; i < customSolution.getMicroservices().size(); i++) {
            var customMicroservice = customSolution.getMicroservices().get(i);
            var classes = new HashSet<OtherClass>();
            for (var fqn : customMicroservice.getFqns()) {
                if (seen.contains(fqn)) {
                    throw new IllegalArgumentException("Class " + fqn + " can only belong to one microservice");
                }
                var clazz = otherClasses.get(fqn);
                if (clazz == null) {
                    if (inputGraph.stream().anyMatch(c -> c.getIdentifier().equals(fqn))) {
                        LOGGER.warn("Input class {} is a data class, ignoring as we don't cluster those.", fqn);
                        continue;
                    }
                    throw new IllegalArgumentException("Class " + fqn + " not found in graph for evaluation " + evaluationId);
                }
                classes.add(clazz);
                seen.add(fqn);
            }
            microservices.add(new Microservice(i, classes));
        }
        if (seen.size() != otherClasses.size()) {
            throw new IllegalArgumentException("Only gave " + seen.size() + " other classes but expected " + inputGraph.size());
        }
        var solution = new Solution();
        solution.setMicroservices(microservices);
        return solution;
    }

    /**
     * Calculates the metric values for the given {@code solutionId} based on the metrics defined in the evaluation
     * identified by the given {@code evaluationId}.
     *
     * @param evaluationId the evaluation's ID to retrieve the metrics from
     * @param solutionId   the solution's ID to calculate metrics for
     * @return the persisted solution with metric values
     */
    public Solution calculateMetrics(UUID evaluationId, UUID solutionId) {
        var solution = getSolution(solutionId);
        // Get evaluation, class dependency graph and metrics
        var evaluation = evaluationService.getEvaluationDeep(evaluationId);
        var clustering = new ClusteringBuilder(solution).build();

        // Calculate metrics
        var metricValues = evaluation.getConfiguration().getMetrics().stream()
                .map(metricType -> Pair.of(metricType, metricType.getMetrics().stream()
                        .mapToDouble(metric -> metric.calculate(clustering))
                        .toArray()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        // Persist
        solution.setMetricValues(metricValues);
        return persistSolution(solution);
    }
}
