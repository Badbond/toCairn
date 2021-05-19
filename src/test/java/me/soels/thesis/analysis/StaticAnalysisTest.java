package me.soels.thesis.analysis;

import me.soels.thesis.model.AnalysisModelBuilder;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;

public class StaticAnalysisTest {
    @Test
    public void testStaticAnalysis() throws URISyntaxException {
        var resource = this.getClass().getClassLoader().getResource("./thesis-project-master.zip");
        var project = Path.of(Objects.requireNonNull(resource).toURI());
        var input = new StaticAnalysisInput(project, JAVA_11);
        // thesis-project-master.zip (28 classes, 64 unique method names):
        //      TODO: Investigate these two, I had numerous difference before.
        //      117 total, 17 unresolved, 68 relevant (excl. self-ref), 29 relationships on 3.18.0 -- 2s
        //      117 total, 17 unresolved, 68 relevant (excl. self-ref), 29 relationships on 3.22.1 -- 1s
        // big-project.zip (2642 classes, 5504 unique method names):
        //      stack overflow error on 3.18
        //      28892 total, 12502 unresolved, 6482 relevant (excl. self-ref), 1760 relationships -- 3m 10s on 3.22.1
        // big-project-cleaned.zip (with generated sources by compilation & excluding jars+test: 4882 classes, 10021 unique method names):
        //      stack overflow error on 3.18
        //      87597 total, 7532 unresolved, 32984 relevant (excl. self-ref), 5186 relationships, -- 6m 35s on 3.22.1
        var analysis = new StaticAnalysis();
        var builder = new AnalysisModelBuilder();
        analysis.analyze(builder, input);
        builder.build();
    }
}
