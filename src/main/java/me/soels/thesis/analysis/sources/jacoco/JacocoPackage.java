package me.soels.thesis.analysis.sources.jacoco;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class JacocoPackage {
    @JsonProperty("sourcefile")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<JacocoSourceFile> sourceFiles;

    @JacksonXmlProperty(isAttribute = true)
    private String name;
}
