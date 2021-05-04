package me.soels.tocairn.solver.ahca;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.tocairn.model.Solution;
import me.soels.tocairn.solver.Clustering;

@Getter
@AllArgsConstructor
public class AHCAClustering {
    private final Clustering clustering;
    private final Solution solution;
    private final double assessedQuality;
    private final double totalQuality;
}
