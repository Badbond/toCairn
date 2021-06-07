package me.soels.thesis.tmp;

import me.soels.thesis.tmp.dtos.EvaluationDto;
import me.soels.thesis.tmp.daos.Evaluation;
import me.soels.thesis.tmp.services.EvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing the entire {@link Evaluation}.
 * <p>
 * TODO:
 * Can Neo4J work with 'multiple analysis' usecase in mind? If so, this is a nice way to go as we can
 * keep everything running and stored and see our results. Otherwise, we can only have one analysis per instance :(
 */
@RestController("/api/evaluation")
public class EvaluationController {
    private final EvaluationService service;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvaluationDto> getAllEvaluations() {
        return service.getEvaluations().stream()
                .map(EvaluationDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EvaluationDto getEvaluation(@PathVariable UUID id) {
        return new EvaluationDto(service.getEvaluation(id));
    }

    @PostMapping
    public EvaluationDto createEvaluation(@RequestBody EvaluationDto dto) {
        return new EvaluationDto(service.createEvaluation(dto.toDao()));
    }

    @PostMapping("/{id}/run")
    public EvaluationDto runEvaluation(@PathVariable UUID id) {
        return new EvaluationDto(service.run(id));
    }

    @PutMapping("/{id}")
    public EvaluationDto updateEvaluation(@PathVariable UUID id, @RequestBody EvaluationDto dto) {
        return new EvaluationDto(service.updateEvaluation(id, dto.toDao()));
    }
}
