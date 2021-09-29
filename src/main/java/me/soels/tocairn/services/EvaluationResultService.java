package me.soels.tocairn.services;

import me.soels.tocairn.api.ResourceNotFoundException;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.EvaluationResult;
import me.soels.tocairn.model.Microservice;
import me.soels.tocairn.model.Solution;
import me.soels.tocairn.repositories.EvaluationResultRepository;
import me.soels.tocairn.repositories.MicroserviceRepository;
import me.soels.tocairn.repositories.SolutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for managing {@link EvaluationResult}
 */
@Service
public class EvaluationResultService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationResultService.class);
    private final EvaluationResultRepository resultRepository;
    private final SolutionRepository solutionRepository;
    private final MicroserviceRepository microserviceRepository;

    public EvaluationResultService(EvaluationResultRepository resultRepository,
                                   SolutionRepository solutionRepository,
                                   MicroserviceRepository microserviceRepository) {
        this.resultRepository = resultRepository;
        this.solutionRepository = solutionRepository;
        this.microserviceRepository = microserviceRepository;
    }

    /**
     * Delete the result with the given {@code id}.
     * <p>
     * Performs cascading delete for the solutions in this results and the clusters in the solutions. The input
     * graph is not deleted.
     *
     * @param id the id of the result to delete
     */
    public void deleteResult(UUID id) {
        var maybeResult = resultRepository.getByIdShallow(id);
        // Delete the clusters associated with all the solutions in this result
        maybeResult.ifPresent(result -> result.getSolutions().stream()
                .flatMap(solution -> solution.getMicroservices().stream())
                .forEach(microserviceRepository::delete));
        // Delete all the solutions
        maybeResult.ifPresent(result -> solutionRepository.deleteAll(result.getSolutions()));
        // Delete the run result itself
        maybeResult.ifPresent(resultRepository::delete);
    }

    public EvaluationResult getShallowResult(UUID id) {
        return resultRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Solution getShallowSolution(UUID id) {
        return solutionRepository.getByIdShallow(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public void persistResult(EvaluationResult result) {
        LOGGER.info("Persisting clustering result(s)");
        LOGGER.info(" - Temporarily storing microservices' classes and resetting them");
        Map<Microservice, List<UUID>> microserviceClasses = new HashMap<>();
        for (var solution : result.getSolutions()) {
            for (var microservice : solution.getMicroservices()) {
                // Store the instance of this microservice and the classes it should reference.
                microserviceClasses.put(microservice, microservice.getClasses().stream()
                        .map(AbstractClass::getId)
                        .collect(Collectors.toCollection(ArrayList::new)));
                // Remove the class references such that we can persist the microservice completely.
                microservice.setClasses(Collections.emptySet());
            }
        }
        LOGGER.info(" - Persisting {} microservices.", microserviceClasses.size());
        microserviceClasses.keySet().parallelStream().forEach(microserviceRepository::save);

        LOGGER.info(" - Temporarily storing solutions' microservices and resetting them");
        Map<Solution, List<UUID>> solutionMicroservices = new HashMap<>();
        for (var solution : result.getSolutions()) {
            // Store the instance of this solution and the microservices it should reference.
            solutionMicroservices.put(solution, solution.getMicroservices().stream()
                    .map(Microservice::getId)
                    .collect(Collectors.toCollection(ArrayList::new)));
            // Remove the microservice references such that we can persist the result and solution completely.
            solution.setMicroservices(Collections.emptyList());
        }

        LOGGER.info(" - Persisting result and its {} solutions.", solutionMicroservices.size());
        resultRepository.save(result);

        LOGGER.info(" - Creating links from solutions to the microservices");
        solutionMicroservices.forEach((solution, microservices) ->
                solutionRepository.createRelationships(solution.getId(), microservices));

        LOGGER.info(" - Creating links from microservices to the classes");
        microserviceClasses.forEach((microservice, classes) ->
                microserviceRepository.createRelationships(microservice.getId(), classes));

        LOGGER.info("Done persisting clustering result(s)");
    }
}
