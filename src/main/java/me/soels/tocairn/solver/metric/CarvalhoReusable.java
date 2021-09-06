package me.soels.tocairn.solver.metric;

import me.soels.tocairn.solver.Clustering;

/**
 * Reusability measurement by Carvalho et al. (2020).
 * <p>
 * This metric should be maximized but, as the MOEA framework only allows for minimization objectives, we negate the
 * value.
 * TODO: Describe how we take class approach here.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE T
 **/
public class CarvalhoReusable implements Metric {
    @Override
    public double calculate(Clustering clustering) {
        return -1 * 0;
    }
}
