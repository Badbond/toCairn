package me.soels.thesis.controllers;

import me.soels.thesis.controllers.dtos.AnalysisDto;
import me.soels.thesis.model.Analysis;
import me.soels.thesis.model.AnalysisRepository;
import me.soels.thesis.util.NonNullPropertyCopyUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing the entire {@link Analysis}.
 * <p>
 * TODO:
 * Can Neo4J work with 'multiple analysis' usecase in mind? If so, this is a nice way to go as we can
 * keep everything running and stored and see our results. Otherwise, we can only have one analysis per instance :(
 *
 */
@RestController("/api/analysis")
public class AnalysisController {
    private final AnalysisRepository repository;

    public AnalysisController(AnalysisRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AnalysisDto> getAllAnalysis() {
        return repository.findAll().stream()
                .map(AnalysisDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public AnalysisDto getAnalysis(@PathVariable UUID id) {
        return new AnalysisDto(requireAnalysis(id));
    }

    @PostMapping
    public AnalysisDto createAnalysis(@RequestBody AnalysisDto analysis) {
        return new AnalysisDto(repository.save(analysis.toDao()));
    }

    @PutMapping("/{id}")
    public AnalysisDto updateAnalysis(@RequestBody AnalysisDto analysis, @PathVariable UUID id) {
        var dao = requireAnalysis(id);
        NonNullPropertyCopyUtil.copyProperties(analysis.toDao(), dao);
        return new AnalysisDto(repository.save(analysis.toDao()));
    }

    @PatchMapping("/{id}")
    public AnalysisDto patchAnalysis(@RequestBody AnalysisDto analysis, @PathVariable UUID id) {
        var dao = requireAnalysis(id);
        NonNullPropertyCopyUtil.copyProperties(analysis.toDao(), dao);
        return new AnalysisDto(repository.save(dao));
    }

    private Analysis requireAnalysis(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
