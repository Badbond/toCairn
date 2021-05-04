package me.soels.tocairn.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.model.Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to write a list of solutions to a temporary file.
 * <p>
 * Can be used when running the application in debug to pause the application and then, using the debugger's variable
 * inspector, writing the result using this static method.
 */
@SuppressWarnings("unused") // For debugging purposes.
public class SolutionToFileWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionToFileWriter.class);

    private SolutionToFileWriter() {
        // Utility class, do not initialise.
    }

    public static void serialise(List<Solution> solutions) {
        var mapper = new ObjectMapper();
        var desRes = new HashMap<Integer, Object>();
        for (int i = 0; i < solutions.size(); i++) {
            var sol = solutions.get(i);
            var desSol = new HashMap<String, Object>();
            desSol.put("i", i);
            desSol.put("metricValues", sol.getMetricValues());
            var desMss = new ArrayList<>();
            for (var ms : sol.getMicroservices()) {
                var desMs = new HashMap<String, Object>();
                desMs.put("i", ms.getMicroserviceNumber());
                desMs.put("classes", ms.getClasses().stream().map(OtherClass::getId).collect(Collectors.toList()));
                desMss.add(desMs);
            }
            desSol.put("microservices", desMss);
            desRes.put(i, desSol);
        }

        try {
            mapper.writeValue(File.createTempFile("tocairn-solutions", ".json"), desRes);
        } catch (IOException e) {
            LOGGER.warn("Could not write solutions to temporary file", e);
        }
    }
}
