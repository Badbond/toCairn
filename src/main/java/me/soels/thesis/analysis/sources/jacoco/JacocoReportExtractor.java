package me.soels.thesis.analysis.sources.jacoco;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JacocoReportExtractor {
    XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .findAndRegisterModules();

    public void extractJaCoCoReport(Path jacocoReport, Map<String, Map<Integer, Integer>> sourceExecutions) throws IOException {
        var report = xmlMapper.readValue(Files.newInputStream(jacocoReport), JacocoReport.class);
        for (var sourcePackage : report.getPackages()) {
            for (var sourceFile : sourcePackage.getSourceFiles()) {
                // TODO: See how we handle inner classes (they are encapsulated in the same source file. Perhaps we can do a startsWith() implementation).
                var fqn = sourcePackage.getName() + "/" + sourceFile.getName().substring(0, sourceFile.getName().length() - 5);
                sourceExecutions.put(fqn, sourceFile.getLines().stream()
                        .collect(Collectors.toMap(
                                line -> Integer.valueOf(line.getNr()),
                                line -> Integer.valueOf(line.getEc())))
                );
            }
        }
    }
}
