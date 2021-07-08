package me.soels.thesis.analysis.sources.jacoco;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class JacocoReportExtractor {
    XmlMapper xmlMapper = (XmlMapper) new XmlMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .findAndRegisterModules();

    public void extractJaCoCoReport(Path jacocoReport, Map<String, Map<Integer, Integer>> sourceExecutions) throws IOException {
        var report = xmlMapper.readValue(Files.newInputStream(jacocoReport), JacocoReport.class);
        var a = 1;
    }
}
