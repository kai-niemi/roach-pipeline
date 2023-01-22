package io.roach.pipeline.expression;

import org.antlr.v4.runtime.Parser;

/**
 * Exception thrown when an expression violates the grammar or cannot
 * be compiled.
 *
 * @author Kai Niemi
 */
public class ExpressionException extends RuntimeException {
    public static ExpressionException create(Parser parser, String message) {
        parser.removeParseListeners();
        return new ExpressionException(message + " [near " + parser.getCurrentToken().getText() + "]");
    }

    public ExpressionException(Throwable cause) {
        super(cause);
    }

    public ExpressionException(String message) {
        super(message);
    }

    public ExpressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
