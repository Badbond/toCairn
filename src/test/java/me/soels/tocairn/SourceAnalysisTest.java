package me.soels.tocairn;

import me.soels.tocairn.analysis.sources.SourceAnalysis;
import me.soels.tocairn.analysis.sources.SourceAnalysisInput;
import me.soels.tocairn.model.EvaluationInputBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class SourceAnalysisTest {
    @Autowired
    private SourceAnalysis analysis;

    @Test
    void testSourceAnalysis() throws URISyntaxException, IOException {
        var resource = this.getClass().getClassLoader().getResource("./thesis-project-master.zip");
        runAnalysis(resource);
    }

    private void runAnalysis(URL resource) throws URISyntaxException, IOException {
        var project = Path.of(Objects.requireNonNull(resource).toURI());
        var jacoco = getJaCoCoXML();
        var input = new SourceAnalysisInput(project, jacoco, JAVA_11, null, List.of(".*Application"), null);
        // thesis-project-master.zip (28 classes, 64 unique method names):
        //      117 total, 17 unresolved, 68 relevant (excl. self-ref), 29 relationships on 3.18.0 -- 2s
        //      117 total, 38 unresolved, 57 relevant (excl. self-ref), 25 relationships on 3.22.1 -- 1s
        // project.zip (2642 classes, 5504 unique method names):
        //      stack overflow error on 3.18
        //      28892 total, 12502 unresolved, 6482 relevant (excl. self-ref), 1760 relationships -- 3m 10s on 3.22.1
        // project-cleaned.zip (with generated sources by compilation & excluding jars+test: 4882 classes, 10021 unique method names):
        //      stack overflow error on 3.18
        //      87597 total, 7532 unresolved, 32984 relevant (excl. self-ref), 5186 relationships, -- 6m 35s on 3.22.1
        var builder = new EvaluationInputBuilder(Collections.emptyList());
        var context = analysis.prepareContext(builder, input);
        analysis.analyzeNodes(context);
        analysis.analyzeEdges(context);
        builder.build();
    }

    private Path getJaCoCoXML() throws URISyntaxException {
        var url = this.getClass().getClassLoader().getResource("./jacoco.xml");
        return url == null ? null : Path.of(url.toURI());
    }
}
