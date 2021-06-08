package me.soels.thesis.analysis;

import me.soels.thesis.model.EvaluationInputBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;

@SpringBootTest
@RunWith(SpringRunner.class)
public class StaticAnalysisTest {
    @Autowired
    private StaticAnalysis analysis;

    @Test
    public void testStaticAnalysis() throws URISyntaxException {
        var resource = this.getClass().getClassLoader().getResource("./big-project-cleaned-4.zip");
        runAnalysis(resource);
    }

    private void runAnalysis(URL resource) throws URISyntaxException {
        var project = Path.of(Objects.requireNonNull(resource).toURI());
        var input = new StaticAnalysisInput(UUID.randomUUID(), project, JAVA_11, null);
        // thesis-project-master.zip (28 classes, 64 unique method names):
        //      117 total, 17 unresolved, 68 relevant (excl. self-ref), 29 relationships on 3.18.0 -- 2s
        //      117 total, 38 unresolved, 57 relevant (excl. self-ref), 25 relationships on 3.22.1 -- 1s
        // big-project.zip (2642 classes, 5504 unique method names):
        //      stack overflow error on 3.18
        //      28892 total, 12502 unresolved, 6482 relevant (excl. self-ref), 1760 relationships -- 3m 10s on 3.22.1
        // big-project-cleaned.zip (with generated sources by compilation & excluding jars+test: 4882 classes, 10021 unique method names):
        //      stack overflow error on 3.18
        //      87597 total, 7532 unresolved, 32984 relevant (excl. self-ref), 5186 relationships, -- 6m 35s on 3.22.1
        var builder = new EvaluationInputBuilder();
        analysis.analyze(builder, input);
        builder.build();
    }
}
