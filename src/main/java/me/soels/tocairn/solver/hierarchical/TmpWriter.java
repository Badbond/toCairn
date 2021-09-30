package me.soels.tocairn.solver.hierarchical;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.soels.tocairn.model.OtherClass;
import me.soels.tocairn.model.Solution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TmpWriter {
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
            mapper.writeValue(File.createTempFile("ahca", "tocairn"), desRes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
