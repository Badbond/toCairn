package me.soels.thesis.tmp;

import me.soels.thesis.tmp.daos.EvaluationResult;
import me.soels.thesis.tmp.dtos.EvaluationResultDto;
import me.soels.thesis.tmp.services.EvaluationResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for listing {@link EvaluationResult}.
 * <p>
 * TODO:
 * How to expose the solutions in the clustering from optimization?
 */
@RestController
@RequestMapping("/api/results")
public class EvaluationResultController {
    private final EvaluationResultService service;

    public EvaluationResultController(EvaluationResultService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvaluationResultDto> getAllEvaluationResults() {
        return service.getResults().stream()
                .map(EvaluationResultDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EvaluationResultDto getEvaluationResult(@PathVariable UUID id) {
        return new EvaluationResultDto(service.getResult(id));
    }

    // TODO: Delete
}
