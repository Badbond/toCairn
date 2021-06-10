package me.soels.thesis.clustering.encoding;

/**
 * The variable type to use with the evolutionary algorithms.
 * <p>
 * Float integers allow for more crossover and mutation operations compared to binary integers and is therefore
 * recommended to support more algorithms.
 */
public enum VariableType {
    BINARY_INT, FLOAT_INT
}
