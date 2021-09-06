package me.soels.tocairn.analysis.sources.jacoco;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class JacocoLine {
    @JacksonXmlProperty(isAttribute = true)
    private String nr;
    @JacksonXmlProperty(isAttribute = true)
    private String ec;
}
