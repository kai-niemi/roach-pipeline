package io.roach.pipeline.util.graph;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * Basic implementation for an undirected graph or directed acyclic graph for
 * resolving dependency ordering through topology sorting (using recursive DFS traversal).
 *
 * @param <N> the node type
 * @param <V> the edge value type
 */
public class Graph<N, V> implements Iterable<N>, ImmutableGraph<N, V> {
    private final Set<N> nodes = new HashSet<>();

    private final Set<Edge<N, V>> edges = new HashSet<>();

    public Graph<N, V> addNode(N node) {
        this.nodes.add(node);
        return this;
    }

    public Edge<N, V> addEdge(N start, N end) {
        return addEdge(start, end, null);
    }

    public Edge<N, V> addEdge(N start, N end, V value) {
        if (!nodes.contains(start)) {
            throw new IllegalStateException("No such node: " + start);
        }
        if (!nodes.contains(end)) {
            throw new IllegalStateException("No such node: " + end);
        }
        if (start.equals(end)) {
            throw new IllegalStateException("Self-reference: "
                    + start + " == " + end);
        }
        Edge<N, V> edge = new Edge<>(start, end, value);
        this.edges.remove(edge);
        this.edges.add(edge);
        return edge;
    }

    @Override
    public boolean contains(N node) {
        return nodes.contains(node);
    }

    @Override
    public Optional<V> edgeValue(N from, N to) {
        return edge(from, to).map(Edge::getValue);
    }

    public Optional<Edge<N, V>> edge(N start, N target) {
        for (Edge<N, V> edge : edges) {
            if (edge.getStart().equals(start) && edge.getEnd().equals(target)) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<N> adjacentNodes(N node) {
        Set<N> adjacent = new HashSet<>();
        for (Edge<N, V> edge : edges) {
            if (edge.getStart().equals(node)) {
                adjacent.add(edge.getEnd());
            }
        }
        return adjacent;
    }

    @Override
    public Set<N> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    @Override
    public Set<Edge<N, V>> edges() {
        return Collections.unmodifiableSet(edges);
    }

    @Override
    public Iterator<N> iterator() {
        return nodes().iterator();
    }

    /**
     * Performs a topological sort on the graph, expected to have
     * directed edges with no cycles (DAG). If a cycle is detected
     * an exception is thrown.
     *
     * @return topologically sorted list of nodes
     * @throws IllegalStateException if the graph contains at least one cycle
     */
    @Override
    public List<N> topologicalSort() {
        Deque<N> visited = new ArrayDeque<>();
        Deque<N> stack = new ArrayDeque<>();
        Deque<N> trail = new ArrayDeque<>();

        nodes().forEach(node -> topologicalSortRecursive(node, visited, stack, trail));

        return List.copyOf(stack);
    }

    private void topologicalSortRecursive(N node, Deque<N> visited, Deque<N> stack, Deque<N> trail) {
        if (!visited.contains(node)) {
            visited.add(node);
            trail.add(node);

            for (N neighbor : adjacentNodes(node)) {
                if (trail.contains(neighbor)) {
                    throw new IllegalStateException("Cycle detected: ["
                            + node.toString() + "->" + neighbor.toString()
                            + "] visited: " + visited);
                }
                if (!visited.contains(neighbor)) {
                    topologicalSortRecursive(neighbor, visited, stack, trail);
                }
            }

            stack.push(node);
            trail.remove(node);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        edges().forEach(e -> {
            if (sb.length() > 0) {
                sb.append(",\n");
            }
            sb.append("  ");
            sb.append(e);
        });
        return "Graph {" +
                "\nnodes = " + StringUtils.collectionToCommaDelimitedString(nodes) +
                ",\nedges = \n" + sb +
                "\n}";
    }
}