package me.soels.thesis.tmp;

import me.soels.thesis.tmp.daos.Evaluation;
import me.soels.thesis.tmp.dtos.EvaluationDto;
import me.soels.thesis.tmp.services.EvaluationConfigurationService;
import me.soels.thesis.tmp.services.EvaluationService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing the entire {@link Evaluation}.
 * <p>
 * TODO:
 * Can Neo4J work with 'multiple analysis' use-case in mind? If so, this is a nice way to go as we can
 * keep everything running and stored and see our results. Otherwise, we can only have one analysis per instance :(
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final EvaluationConfigurationService configurationService;
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationConfigurationService configurationService, EvaluationService evaluationService) {
        this.configurationService = configurationService;
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public List<EvaluationDto> getAllEvaluations() {
        return evaluationService.getEvaluations().stream()
                .map(EvaluationDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EvaluationDto getEvaluation(@PathVariable UUID id) {
        return new EvaluationDto(evaluationService.getEvaluation(id));
    }

    @PostMapping
    public EvaluationDto createEvaluation(@Valid @RequestBody EvaluationDto dto) {
        var configuration = configurationService.createConfiguration(dto.getConfiguration().toDao());
        return new EvaluationDto(evaluationService.createEvaluation(dto.toDao(), configuration));
    }

    @PostMapping("/{id}/run")
    public EvaluationDto runEvaluation(@PathVariable UUID id) {
        return new EvaluationDto(evaluationService.run(id));
    }

    @PutMapping("/{id}")
    public EvaluationDto updateEvaluation(@PathVariable UUID id, @Valid @RequestBody EvaluationDto dto) {
        return new EvaluationDto(evaluationService.updateEvaluation(id, dto.toDao()));
    }
}
