package me.soels.tocairn.solver.moea;

/**
 * The encoding type used in the problem model for clustering.
 * <p>
 * The naming of these types of encoding is based off of the following work: 'Mukhopadhyay, A., Maulik, U., &
 * Bandyopadhyay, S. (2015). A survey of multiobjective evolutionary clustering.<i>ACM Computing Surveys (CSUR),
 * 47</i>(4), 1-46.'.
 * <p>
 * Both encoding types are part of point-based gene encoding. In this type of encoding, the data points themselves
 * are part of the encoding instead of the clusters resulting from the algorithm (prototype-based encoding). Therefore,
 * each gene represents a class that we are trying to cluster. We represent the genome as a 0-indexed list of genes.
 * <p>
 * In 'cluster label-based encoding', the value of the gene represents the cluster that that data point is in. The
 * cluster is represented by an integer number in our case. Our approach does not impose limits on the number of
 * clusters and therefore the values of the gene, {@code x}, ranges from {@code 0<=x<n} with {@code n} being the
 * amount of data points (every data point in its own cluster).
 * <p>
 * In 'locus-based adjacency encoding', the value of the gene instead represents a link to another gene. Therefore,
 * a directed graph is constructed between data points. We use the gene index of the linked gene as the value. The
 * value of the gene, {@code x}, therefore ranges from {@code 0<=x<n} with {@code n} being the amount of data points.
 * All connected points represent a cluster.
 */
public enum EncodingType {
    CLUSTER_LABEL, GRAPH_ADJECENCY
}
