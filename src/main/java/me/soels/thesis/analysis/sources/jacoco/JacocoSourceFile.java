package me.soels.thesis.analysis.sources.jacoco;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public final class JacocoSourceFile {
    @JsonProperty("line")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<JacocoLine> lines;

    @JacksonXmlProperty(isAttribute = true)
    private String name;
}
