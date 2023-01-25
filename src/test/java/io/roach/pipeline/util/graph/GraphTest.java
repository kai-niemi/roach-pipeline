package io.roach.pipeline.util.graph;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GraphTest {
    @Test
    public void givenDirectedGraph_whenTopologicalSort_thenPrintValuesSorted() {
        Graph<String, String> graph = new Graph<>();

        graph.addNode("order");
        graph.addNode("order_item");
        graph.addNode("product");
        graph.addNode("customer");
        graph.addNode("category");

        graph.addEdge("order_item", "order", "fk_order_item_order");
        graph.addEdge("order_item", "product", "fk_order_item_product");
        graph.addEdge("order", "customer", "fk_order_product");
        graph.addEdge("product", "category", "fk_product_category");

        List<String> result = graph.topologicalSort(false);

        Assertions.assertEquals(Arrays.asList("order_item", "order", "customer", "product", "category"), result);

        System.out.println(graph);
    }

    @Test
    public void givenDirectedGraph_whenTopologicalSort_thenPrintValuesSortedAgain() {
        Graph<String, Integer> graph = new Graph<>();

        graph.addNode("A");
        graph.addNode("B");
        graph.addNode("C");
        graph.addNode("D");
        graph.addNode("E");

        graph.addEdge("A", "B", 1);
        graph.addEdge("A", "C");
        graph.addEdge("A", "D", 2);
        graph.addEdge("B", "D");
        graph.addEdge("C", "E", 3);
        graph.addEdge("D", "E");

        List<String> result = graph.topologicalSort(false);
        Assertions.assertEquals(Arrays.asList("A", "C", "B", "D", "E"), result);

        System.out.println(graph);
    }

    @Test
    public void givenCyclicGraph_whenTopologicalSort_thenThrowException() {
        Graph<String, Double> graph = new Graph<>();

        graph.addNode("A");
        graph.addNode("B");
        graph.addNode("C");
        graph.addNode("D");
        graph.addNode("E");

        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("A", "D");
        graph.addEdge("B", "D");
        graph.addEdge("C", "E");
        graph.addEdge("D", "E");

        graph.addEdge("E", "A"); // cycle

        System.out.println(graph);

        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> graph.topologicalSort(false));

        Assertions.assertEquals("Cycle detected: [E->A] visited: [A, B, D, E]", exception.getMessage());
    }
}
