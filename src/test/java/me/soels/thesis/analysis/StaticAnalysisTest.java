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
        var resource = this.getClass().getClassLoader().getResource("./big-project-cleaned.zip");
        var project = Path.of(Objects.requireNonNull(resource).toURI());
        var input = new StaticAnalysisInput(project, JAVA_11, null);
        // thesis-project-master.zip (28 classes, 64 unique method names):
        //      117 total, 17 unresolved, 68 relevant (excl. self-ref), 29 relationships on 3.18.0 -- 2s
        //      117 total, 38 unresolved, 57 relevant (excl. self-ref), 25 relationships on 3.22.1 -- 1s
        // big-project.zip (2642 classes, 5504 unique method names):
        //      stack overflow error on 3.18
        //      28892 total, 12502 unresolved, 6482 relevant (excl. self-ref), 1760 relationships -- 3m 10s on 3.22.1
        // big-project-cleaned.zip (with generated sources by compilation & excluding jars+test: 4882 classes, 10021 unique method names):
        //      stack overflow error on 3.18
        //      87597 total, 7532 unresolved, 32984 relevant (excl. self-ref), 5186 relationships, -- 6m 35s on 3.22.1
        //      87586 total, 7532 unresolved, 32984 relevant (excl. self-ref), 5186 relationships, -- 2m 01s parallelized
        //      87570 total, 7532 unresolved, 32966 relevant (excl. self-ref), 5186 relationships, -- 2m 13s parallelized
        //      43222 total, 6763 unresolved, 10298 relevant (excl. self-ref), 3096 relationships, -- 3m 54s on 3.22.1 (16 cores instead of 8)
        // big-project-cleaned-2.zip (with generated sources by compilation & excluding .jar, /test/, /classes/,
        //                      /generated-test-sources/, /test-classes/: 4883 classes, 10042 unique method names):
        //      43222 total, 6763 unresolved, 10298 relevant (excl. self-ref), 3096 relationships, -- 2m 43s on 3.22.1 (16 cores instead of 8)
        // 2014 dependencies from data class to abstract class. -- 22403 calls ignored -- Difference in 5186-3096-2014=76 is due to changes in platform.

        // 18K calls saved, 150 less dependencies.
        var analysis = new StaticAnalysis();
        var builder = new AnalysisModelBuilder();
        analysis.analyze(builder, input);
        builder.build();
    }
}
