package me.soels.thesis;/* Copyright 2009-2020 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Demonstrates how a new problem is defined and used within the MOEA
 * Framework.
 */
public class TestingMOEA {

    public static void main(String[] args) {
        var objectives = List.of(new CohesionObjective(), new CouplingObjective());
        var input = new ApplicationInput();

        // TODO:
        //  Instead of random, we want to make smarter initialization by doing the following:
        //      - Select n random classes and start traversing every class not already visited
        //      - Unvisited classes get assigned a random cluster.
        //  This is hardcoded in the AlgorithmProviders. Therefore, we need to think of some injection (e.g. aspects /
        //  custom providers). See https://github.com/MOEAFramework/MOEAFramework/issues/51#issuecomment-223448440
        NondominatedPopulation result = new Executor()
                .withProblem(new ClusteringProblem(objectives, input, EncodingType.CLUSTER_LABEL))
                .withAlgorithm("NSGAII")
                .withMaxEvaluations(10000)
                .run();

        //display the results
        printResults(result, objectives);
    }

    private static void printResults(NondominatedPopulation result, List<Objective> objectives) {
        var objectivesString = objectives.stream()
                .map(objective -> objective.getClass().getSimpleName())
                .collect(Collectors.joining("  "));
        System.out.format(objectivesString + "%n");

        for (Solution solution : result) {
            Object[] objectivesResult = new Object[]{solution.getObjectives()};
            System.out.format("%.4f      ".repeat(objectives.size()) + "%n", objectivesResult);
        }
    }

}
