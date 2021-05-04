package me.soels.tocairn.analysis.dynamic;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import me.soels.tocairn.model.AbstractClass;
import me.soels.tocairn.model.EvaluationInputBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicAnalysis.class);

    public void analyze(EvaluationInputBuilder modelBuilder, DynamicAnalysisInput input) {
        var jfrDirectoryPath = input.getPathToJfrDirectory();
        var namespace = input.getNamespace();
        if (!Files.exists(jfrDirectoryPath)) {
            throw new IllegalArgumentException("The given directory does not exist for path " + jfrDirectoryPath);
        } else if (!Files.isDirectory(jfrDirectoryPath)) {
            throw new IllegalArgumentException("The given path " + jfrDirectoryPath + " does not point towards a directory");
        }

        Map<AbstractClass, Long> knownClasses = readJFRFiles(modelBuilder, jfrDirectoryPath, namespace);
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

    private Map<AbstractClass, Long> readJFRFiles(EvaluationInputBuilder modelBuilder, Path jfrDirectoryPath, String namespace) {
        var jfrFiles = FileUtils.listFiles(jfrDirectoryPath.toFile(),
                new RegexFileFilter(".*\\.jfr"),
                DirectoryFileFilter.DIRECTORY
        );
        if (jfrFiles.isEmpty()) {
            throw new IllegalStateException("Could not find any .jfr files in given directory " + jfrDirectoryPath);
        }

        Map<AbstractClass, Long> knownClasses = new ConcurrentHashMap<>();
        jfrFiles.parallelStream()
                .forEach(jfrFile -> {
                    LOGGER.info("Reading JFR file {}", jfrFile);
                    try {
                        readJFRFile(modelBuilder, jfrFile.toPath(), namespace, knownClasses);
                    } catch (Exception e) {
                        LOGGER.warn("JFR file {} could not be (fully) analysed.", jfrFile, e);
                    }
                });
        return knownClasses;
    }

    private void readJFRFile(EvaluationInputBuilder modelBuilder, Path jfrFilePath, String namespace, Map<AbstractClass, Long> knownClasses) throws IOException {
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
