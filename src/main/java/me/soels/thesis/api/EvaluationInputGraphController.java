package me.soels.thesis.api;

import me.soels.thesis.api.dtos.*;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.DataClass;
import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.model.OtherClass;
import me.soels.thesis.services.EvaluationInputService;
import me.soels.thesis.services.EvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for exposing the {@link EvaluationInput}.
 * <p>
 * This controller has endpoints to expose the nodes and the edges of the graph.
 */
@RestController
@RequestMapping("/api/evaluation/{evaluationId}/graph")
public class EvaluationInputGraphController {
    private final EvaluationService evaluationService;
    private final EvaluationInputService inputService;

    public EvaluationInputGraphController(EvaluationService evaluationService, EvaluationInputService inputService) {
        this.evaluationService = evaluationService;
        this.inputService = inputService;
    }

    @GetMapping("/nodes")
    public List<AbstractClassDto> getNodes(@PathVariable UUID evaluationId) {
        var graph = inputService.getInput(evaluationService.getEvaluation(evaluationId));
        return graph.getAllClasses().stream()
                .map(this::mapClass)
                .collect(Collectors.toUnmodifiableList());
    }

    @GetMapping("/edges")
    public List<AbstractRelationshipDto> getEdges(@PathVariable UUID evaluationId) {
        var graph = inputService.getInput(evaluationService.getEvaluation(evaluationId));
        List<AbstractRelationshipDto> edges = graph.getAllClasses().stream()
                .flatMap(abstractClass -> abstractClass.getDependenceRelationships().stream()
                        .map(relationship -> new DependenceRelationshipDto(abstractClass.getIdentifier(),
                                relationship.getCallee().getIdentifier(),
                                relationship.getFrequency())))
                .collect(Collectors.toList());
        edges.addAll(graph.getOtherClasses().stream()
                .flatMap(otherClass -> otherClass.getDataRelationships().stream()
                        .map(relationship -> new DataRelationshipDto(otherClass.getIdentifier(),
                                relationship.getCallee().getIdentifier(),
                                relationship.getFrequency(),
                                relationship.getType())))
                .collect(Collectors.toList()));
        return edges;
    }

    private AbstractClassDto mapClass(AbstractClass clazz) {
        if (clazz instanceof DataClass) {
            return new DataClassDto((DataClass) clazz);
        } else if (clazz instanceof OtherClass) {
            return new OtherClassDto((OtherClass) clazz);
        } else {
            throw new IllegalStateException("Unsupported class type found " + clazz.getClass().getSimpleName());
        }
    }
}
