package me.soels.thesis.analysis.dynamic;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import me.soels.thesis.model.AbstractClass;
import me.soels.thesis.model.EvaluationInputBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
public class DynamicAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicAnalysis.class);

    public void analyze(EvaluationInputBuilder modelBuilder, DynamicAnalysisInput input) throws IOException {
        var jfrFilePath = input.getPathToJfrFile();
        var namespace = input.getNamespace();
        if (!jfrFilePath.getFileName().toString().toLowerCase().endsWith(".jfr")) {
            throw new IllegalArgumentException("The path does not refer to a .jfr file, for path " + jfrFilePath);
        } else if (!Files.exists(jfrFilePath)) {
            throw new IllegalArgumentException("The JFR file does not exist for path " + jfrFilePath);
        }

        Map<AbstractClass, Long> knownClasses = new HashMap<>();
        try (var jfrFile = new RecordingFile(jfrFilePath)) {
            while (jfrFile.hasMoreEvents()) {
                var event = jfrFile.readEvent();
                var eventType = EventType.parse(event);
                if (eventType == EventType.OTHER) {
                    continue;
                }
                analyseTlabEvent(modelBuilder, knownClasses, namespace, event, eventType);
            }
        }
        var average = Math.round(knownClasses.values().stream()
                .mapToLong(value -> value)
                .average()
                .orElse(24));

        if (knownClasses.isEmpty()) {
            LOGGER.warn("No size could be determined from JFR analysis. Defaulting sizes 24 bytes (class overhead).");
        } else {
            LOGGER.info("Determined size for {} out of {} classes. Average: {}.",
                    knownClasses.size(), modelBuilder.getClasses().size(), average);
        }

        modelBuilder.getClasses().forEach(clazz ->
                clazz.setSize(knownClasses.getOrDefault(clazz, average)));
    }

    private void analyseTlabEvent(EvaluationInputBuilder modelBuilder,
                                  Map<AbstractClass, Long> knownClasses,
                                  String namespace,
                                  RecordedEvent event,
                                  EventType eventType) {
        var className = event.getClass("objectClass").getName();
        if (!className.startsWith(namespace)) {
            LOGGER.trace("Skipping TLAB event for class {} as it is not in the defined namespace.", className);
            return;
        }
        var matchingClass = modelBuilder.getClasses().stream()
                .filter(clazz -> clazz.getIdentifier().equals(className))
                .findFirst();
        if (matchingClass.isEmpty()) {
            // This can also be anonimized subclasses such as used in lambda's.
            LOGGER.debug("Could not find class {} from JFR event in our graph", className);
            return;
        }

        if (eventType == EventType.IN_TLAB) {
            knownClasses.put(matchingClass.get(), event.getLong("tlabSize"));
        } else {
            knownClasses.put(matchingClass.get(), event.getLong("allocationSize"));
        }
    }

    private enum EventType {
        IN_TLAB, OUTSIDE_TLAB, OTHER;

        private static EventType parse(RecordedEvent event) {
            switch (event.getEventType().getName()) {
                case "jdk.ObjectAllocationInNewTLAB":
                    return IN_TLAB;
                case "jdk.ObjectAllocationOutsideTLAB":
                    return OUTSIDE_TLAB;
                default:
                    return OTHER;
            }
        }
    }
}
