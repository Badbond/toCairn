package me.soels.thesis.analysis;

import com.github.javaparser.ParserConfiguration;

import java.nio.file.Path;

public final class StaticAnalysisInput {
    private final Path pathToZip;
    private final ParserConfiguration.LanguageLevel languageLevel;

    public StaticAnalysisInput(Path pathToZip, ParserConfiguration.LanguageLevel languageLevel) {
        this.pathToZip = pathToZip;
        this.languageLevel = languageLevel;
    }

    public Path getPathToZip() {
        return pathToZip;
    }

    public ParserConfiguration.LanguageLevel getLanguageLevel() {
        return languageLevel;
    }
}
