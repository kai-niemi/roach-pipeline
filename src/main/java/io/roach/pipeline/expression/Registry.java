package io.roach.pipeline.expression;

import io.roach.pipeline.expression.parser.RuleExpressionParseTreeListener;

/**
 * An interface used by {@link RuleExpressionParseTreeListener} for resolving identifiers
 * in expressions to object references or callback functions. Object references can primitives of type
 * Number, String, Boolean, LocalDate, LocalTime, LocalDateTime or a collection of these.
 *
 * @author Kai Niemi
 * @see Function
 * @see RuleExpressionParseTreeListener
 */
public interface Registry {
    /**
     * Resolves a given variable name.
     *
     * @param id the variable identifier
     * @return value resolved, can be null
     */
    Object getVariable(String id);

    /**
     * Resolves a function by given identifier.
     *
     * @param id the function identifier
     * @return function resolved
     */
    Function getFunction(String id);
}
