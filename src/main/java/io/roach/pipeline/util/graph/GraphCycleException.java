package io.roach.pipeline.util.graph;

public class GraphCycleException extends RuntimeException {
    public GraphCycleException(String message) {
        super(message);
    }
}
