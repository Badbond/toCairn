package me.soels.tocairn.api;

import me.soels.tocairn.api.dtos.*;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.DataClass;
import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.services.EvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.soels.tocairn.util.GenericCollectionExtractor.extractType;

/**
 * Controller for exposing the graph stored in the {@link EvaluationInput}.
 * <p>
 * This controller has endpoints to expose the nodes and the edges of the graph.
 */
@RestController
@RequestMapping("/api/evaluation/{evaluationId}/graph")
public class GraphController {
    private final EvaluationService evaluationService;

    public GraphController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/nodes")
    public List<AbstractClassDto> getNodes(@PathVariable UUID evaluationId) {
        return evaluationService.getEvaluationDeep(evaluationId).getInputs().stream()
                .map(this::mapClass)
                .collect(Collectors.toUnmodifiableList());
    }

    @GetMapping("/edges")
    public List<AbstractRelationshipDto> getEdges(@PathVariable UUID evaluationId) {
        var classes = evaluationService.getEvaluationDeep(evaluationId).getInputs();
        List<AbstractRelationshipDto> edges = classes.stream()
                .flatMap(abstractClass -> abstractClass.getDependenceRelationships().stream()
                        .map(relationship -> new DependenceRelationshipDto(abstractClass.getIdentifier(),
                                relationship)))
                .collect(Collectors.toList());
        edges.addAll(extractType(classes, OtherClass.class).stream()
                .flatMap(otherClass -> otherClass.getDataRelationships().stream()
                        .map(relationship -> new DataRelationshipDto(otherClass.getIdentifier(), relationship)))
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
