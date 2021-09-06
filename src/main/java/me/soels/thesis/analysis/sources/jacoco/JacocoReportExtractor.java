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
    private final XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .findAndRegisterModules();

    public void extractJaCoCoReport(Path jacocoReport, Map<String, Map<Integer, Long>> sourceExecutions) throws IOException {
        var report = xmlMapper.readValue(Files.newInputStream(jacocoReport), JacocoReport.class);
        for (var sourcePackage : report.getPackages()) {
            for (var sourceFile : sourcePackage.getSourceFiles()) {
                var fqn = sourcePackage.getName().replace('/', '.') + "." +
                        sourceFile.getName().substring(0, sourceFile.getName().length() - 5);
                sourceExecutions.put(fqn, sourceFile.getLines().stream()
                        .collect(Collectors.toMap(
                                line -> Integer.valueOf(line.getNr()),
                                line -> Long.valueOf(line.getEc())))
                );
            }
        }
    }
}
