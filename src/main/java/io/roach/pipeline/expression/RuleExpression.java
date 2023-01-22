package io.roach.pipeline.expression;

import java.math.BigDecimal;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import io.roach.pipeline.expression.parser.FailFastErrorStrategy;
import io.roach.pipeline.expression.parser.RuleExpressionParseTreeListener;

/**
 * Parse and evaluate logical rule expressions.
 * <p/>
 * See the ANTLR4 grammar for specifics.
 *
 * @author Kai Niemi
 */
public class RuleExpression {
    public static final Registry EMPTY_REGISTRY = new MapRegistry();

    /**
     * Parse an expression for syntax validation.
     *
     * @param expression the expression
     * @return true if the expression is valid
     */
    public static boolean isValid(String expression) {
        try {
            evaluate(expression);
            return true;
        } catch (ExpressionException ex) {
            return false;
        }
    }

    /**
     * Parse and evaluate a rule expression.
     *
     * @param expression the expression
     * @return the binary outcome
     * @throws ExpressionException if the expression break grammar rules
     */
    public static Object evaluate(String expression) {
        return evaluate(expression, Object.class);
    }

    /**
     * Parse and evaluate a rule expression.
     *
     * @param expression the expression
     * @param type the type that the result object is expected to match
     * @return the result object of the expression
     * @throws ExpressionException if the expression break grammar rules
     */
    public static <T> T evaluate(String expression, Class<T> type) {
        return evaluate(expression, type, EMPTY_REGISTRY);
    }

    /**
     * Parse and evaluate a rule expression.
     *
     * @param expression the expression
     * @param type the type that the result object is expected to match
     * @param registry callback for resolving expression variables and functions
     * @return the result object of the expression
     * @throws ExpressionException if the expression break grammar rules
     */
    public static <T> T evaluate(String expression, Class<T> type, Registry registry) {
        RuleExpressionParser parser = createParser(expression);

        RuleExpressionParseTreeListener listener = new RuleExpressionParseTreeListener(parser, registry);
        parser.addParseListener(listener);
        parser.evaluate();

        Object top = listener.pop();
        if (top instanceof BigDecimal) {
            String bd = top.toString();
            try {
                top = Integer.parseInt(bd);
            } catch (NumberFormatException e) {
                top = Double.parseDouble(bd);
            }
        }
        return type.cast(top);
    }

    private static RuleExpressionParser createParser(String expression) {
        final FailFastErrorStrategy errorStrategy = new FailFastErrorStrategy();

        RuleExpressionLexer lexer
                = new RuleExpressionLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorStrategy);

        RuleExpressionParser parser
                = new RuleExpressionParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(errorStrategy);
        parser.addErrorListener(errorStrategy);

        return parser;
    }
}