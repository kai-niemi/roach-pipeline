package io.roach.pipeline.expression;

import java.util.HashMap;
import java.util.Map;

/**
 * A mutable variable resolver that wraps a standard Map collection.
 *
 * @author Kai Niemi
 */
public class MapRegistry implements Registry {
    private final Map<String, Object> variables = new HashMap<>();

    private final Map<String, Function> functions = new HashMap<>();

    public MapRegistry addVariable(String id, Object value) {
        this.variables.putIfAbsent(id, value);
        return this;
    }

    public MapRegistry addFunction(String id, Function function) {
        this.functions.putIfAbsent(id, function);
        return this;
    }

    @Override
    public Object getVariable(String id) {
        if (!variables.containsKey(id)) {
            throw new ExpressionException("No such variable: " + id);
        }
        return variables.get(id);
    }

    @Override
    public Function getFunction(String id) {
        if (!functions.containsKey(id)) {
            throw new ExpressionException("No such function: " + id);
        }
        return functions.get(id);
    }

    public Iterable<String> variableNames() {
        return variables.keySet();
    }

    public Iterable<String> functionNames() {
        return functions.keySet();
    }
}
