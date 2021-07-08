package me.soels.thesis.analysis.sources.jacoco;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class JacocoReport {
    @JsonProperty("package")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<JacocoPackage> packages;
}
