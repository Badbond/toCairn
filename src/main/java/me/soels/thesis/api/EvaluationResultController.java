package me.soels.thesis.api;

import me.soels.thesis.api.dtos.EvaluationResultDto;
import me.soels.thesis.model.EvaluationResult;
import me.soels.thesis.services.EvaluationResultService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Controller for listing {@link EvaluationResult}.
 * <p>
 * TODO: How to expose the solutions in the clustering from optimization?
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

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteEvaluationResult(@PathVariable UUID id) {
        service.deleteResult(id);
    }
}
