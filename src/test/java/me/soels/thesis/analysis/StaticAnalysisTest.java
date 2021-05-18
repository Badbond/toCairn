package me.soels.thesis.analysis;

import me.soels.thesis.model.AnalysisModelBuilder;
import org.junit.Test;

import java.nio.file.Path;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_11;

public class StaticAnalysisTest {
    @Test
    public void testStaticAnalysis() {
        var input = new StaticAnalysisInput(Path.of("/home/badbond/Downloads/thesis-project-master.zip"), JAVA_11);
        var analysis = new StaticAnalysis();
        var builder = new AnalysisModelBuilder();
        analysis.analyze(builder, input);
        builder.build();
    }
}
