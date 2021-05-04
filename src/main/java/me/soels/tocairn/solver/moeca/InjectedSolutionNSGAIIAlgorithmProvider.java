package me.soels.tocairn.solver.moeca;

import me.soels.tocairn.model.EvaluationInput;
import me.soels.tocairn.model.Solution;
import me.soels.tocairn.solver.ClusteringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.algorithm.ReferencePointNondominatedSortingPopulation;
import org.moeaframework.algorithm.StandardAlgorithms;
import org.moeaframework.core.*;
import org.moeaframework.core.comparator.AggregateConstraintComparator;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.CrowdingComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.InjectedInitialization;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.util.TypedProperties;

import javax.annotation.Nonnull;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.soels.tocairn.util.Constants.NSGAIII_STRING;
import static me.soels.tocairn.util.Constants.NSGAII_STRING;

/**
 * Custom {@link AlgorithmProvider} to allow initialization of NSGA-II with a non-random population based on the given
 * {@link Solution}.
 * <p>
 * Currently only allows {@link EncodingType#CLUSTER_LABEL} encoding.
 *
 * @see StandardAlgorithms
 */
public class InjectedSolutionNSGAIIAlgorithmProvider extends AlgorithmProvider {
    private static final String POP_SIZE = "populationSize";
    private final Solution solution;
    private final EvaluationInput input;
    private final boolean all;

    public InjectedSolutionNSGAIIAlgorithmProvider(EvaluationInput input, Solution solution, boolean all) {
        this.solution = solution;
        this.input = input;
        this.all = all;
    }

    @Override
    public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
        if (NSGAII_STRING.equals(name)) {
            return newNSGAII(new TypedProperties(properties), problem);
        } else if (NSGAIII_STRING.equals(name)) {
            return newNSGAIII(new TypedProperties(properties), problem);
        }
        return null;
    }

    /**
     * Create NSGA-II algorithm.
     * <p>
     * Based on NSGA-II implementation in {@link StandardAlgorithms} but altered to allow for solution injection.
     *
     * @param properties the additional properties to set
     * @param problem    the problem instance
     * @return the algorithm
     */
    private Algorithm newNSGAII(TypedProperties properties, Problem problem) {
        int populationSize = (int) properties.getDouble(POP_SIZE, 100);

        // Overwriting this.
        Initialization initialization = getInitialization(problem, populationSize);

        NondominatedSortingPopulation population =
                new NondominatedSortingPopulation();

        TournamentSelection selection = null;

        if (properties.getBoolean("withReplacement", true)) {
            selection = new TournamentSelection(2, new ChainedComparator(
                    new ParetoDominanceComparator(),
                    new CrowdingComparator()));
        }

        Variation variation = OperatorFactory.getInstance().getVariation(null, properties, problem);

        return new NSGAII(problem, population, null, selection, variation, initialization);
    }

    /**
     * Create NSGA-III algorithm.
     * <p>
     * Based on NSGA-III implementation in {@link StandardAlgorithms} but altered to allow for solution injection.
     *
     * @param properties the additional properties to set
     * @param problem    the problem instance
     * @return the algorithm
     */
    @SuppressWarnings("java:S3776") // Copied code from library, we allow this complexity.
    private Algorithm newNSGAIII(TypedProperties properties, Problem problem) {
        int divisionsOuter = 4;
        int divisionsInner = 0;

        if (properties.contains("divisionsOuter") && properties.contains("divisionsInner")) {
            divisionsOuter = (int) properties.getDouble("divisionsOuter", 4);
            divisionsInner = (int) properties.getDouble("divisionsInner", 0);
        } else if (properties.contains("divisions")) {
            divisionsOuter = (int) properties.getDouble("divisions", 4);
        } else if (problem.getNumberOfObjectives() == 1) {
            divisionsOuter = 100;
        } else if (problem.getNumberOfObjectives() == 2) {
            divisionsOuter = 99;
        } else if (problem.getNumberOfObjectives() == 3) {
            divisionsOuter = 12;
        } else if (problem.getNumberOfObjectives() == 4) {
            divisionsOuter = 8;
        } else if (problem.getNumberOfObjectives() == 5) {
            divisionsOuter = 6;
        } else if (problem.getNumberOfObjectives() == 6) {
            divisionsInner = 1;
        } else if (problem.getNumberOfObjectives() >= 7 && problem.getNumberOfObjectives() <= 10) {
            divisionsOuter = 3;
            divisionsInner = 2;
        } else {
            divisionsOuter = 2;
            divisionsInner = 1;
        }

        int populationSize;

        if (properties.contains(POP_SIZE)) {
            populationSize = (int) properties.getDouble(POP_SIZE, 100);
        } else {
            // compute number of reference points
            populationSize = (int) (CombinatoricsUtils.binomialCoefficient(problem.getNumberOfObjectives() + divisionsOuter - 1, divisionsOuter) +
                    (divisionsInner == 0 ? 0 : CombinatoricsUtils.binomialCoefficient(problem.getNumberOfObjectives() + divisionsInner - 1, divisionsInner)));

            // round up to a multiple of 4
            populationSize = (int) Math.ceil(populationSize / 4d) * 4;
        }

        // Altered this bit to allow injection of solution
        Initialization initialization = getInitialization(problem, populationSize);

        ReferencePointNondominatedSortingPopulation population = new ReferencePointNondominatedSortingPopulation(
                problem.getNumberOfObjectives(), divisionsOuter, divisionsInner);

        Selection selection;

        if (problem.getNumberOfConstraints() == 0) {
            selection = (arity, population1) -> {
                org.moeaframework.core.Solution[] result = new org.moeaframework.core.Solution[arity];

                for (int i = 0; i < arity; i++) {
                    result[i] = population1.get(PRNG.nextInt(population1.size()));
                }

                return result;
            };
        } else {
            selection = new TournamentSelection(2, new ChainedComparator(
                    new AggregateConstraintComparator(),
                    (solution1, solution2) -> PRNG.nextBoolean() ? -1 : 1));
        }

        // disable swapping variables in SBX operator to remain consistent with
        // Deb's implementation (thanks to Haitham Seada for identifying this
        // discrepancy)
        if (!properties.contains("sbx.swap")) {
            properties.setBoolean("sbx.swap", false);
        }

        if (!properties.contains("sbx.distributionIndex")) {
            properties.setDouble("sbx.distributionIndex", 30.0);
        }

        if (!properties.contains("pm.distributionIndex")) {
            properties.setDouble("pm.distributionIndex", 20.0);
        }

        Variation variation = OperatorFactory.getInstance().getVariation(null,
                properties, problem);

        return new NSGAII(problem, population, null, selection, variation,
                initialization);
    }

    @Nonnull
    private RandomInitialization getInitialization(Problem problem, int populationSize) {
        var injectedSolution = problem.newSolution();
        var clustering = new ClusteringBuilder(solution).build().getByClass().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey().getIdentifier(), entry.getValue()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        for (int i = 0; i < injectedSolution.getNumberOfVariables(); i++) {
            var clazz = input.getOtherClasses().get(i);
            var value = clustering.get(clazz.getIdentifier());
            var variable = injectedSolution.getVariable(i);
            if (!(variable instanceof RealVariable)) {
                throw new IllegalStateException("Cluster label encoding should operate on real variables representing integers");
            }
            // Not completely sure whether this works in terms of the int to real encoding in MOEA Framework
            ((RealVariable) variable).setValue(value);
        }

        if (all) {
            var allSols = IntStream.range(0, populationSize)
                    .mapToObj(i -> injectedSolution.deepCopy())
                    .collect(Collectors.toList());
            return new InjectedInitialization(problem, populationSize, allSols);
        }

        return new InjectedInitialization(problem, populationSize, injectedSolution);
    }
}
