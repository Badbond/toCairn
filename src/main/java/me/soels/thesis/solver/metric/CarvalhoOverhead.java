package me.soels.thesis.solver.metric;

import me.soels.thesis.model.EvaluationInput;
import me.soels.thesis.solver.Clustering;

/**
 * Overhead measurement by Carvalho et al. (2020).
 * <p>
 * They calculate this metric on a method-unit instead of class-unit. Therefore, they measure how much data is shared
 * between methods. Instead, we measure how much data is shared between classes by summarizing over data shared
 * between all method calls in a class.
 * <p>
 * See related work 'Carvalho, L., Garcia, A., Colanzi, T. E., Assunção, W. K., Pereira, J. A., Fonseca, B., ... &
 * Lucena, C. (2020, September). On the Performance and Adoption of Search-Based Microservice Identification with
 * toMicroservices. In <i>2020 IEEE International Conference on Software Maintenance and Evolution (ICSME)</i>
 * (pp. 569-580). IEEE.' and 'Chidamber, S. R., & Kemerer, C. F. (1994). A metrics suite for object oriented design.
 * <i>IEEE T
 **/
public class CarvalhoOverhead implements Metric {
    @Override
    public double calculate(Clustering clustering, EvaluationInput evaluationInput) {
        return -1 * 0;
    }
}
