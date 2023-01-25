package io.roach.pipeline.util.graph;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ImmutableGraph<N, V> {
    boolean contains(N node);

    Set<N> nodes();

    Set<Edge<N, V>> edges();

    Optional<V> edgeValue(N from, N to);

    Set<N> adjacentNodes(N node);

    List<N> topologicalSort(boolean reverse);
}
