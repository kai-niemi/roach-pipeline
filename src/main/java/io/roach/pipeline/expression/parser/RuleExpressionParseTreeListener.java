package io.roach.pipeline.expression.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roach.pipeline.expression.ExpressionException;
import io.roach.pipeline.expression.Function;
import io.roach.pipeline.expression.Registry;
import io.roach.pipeline.expression.RuleExpressionBaseListener;
import io.roach.pipeline.expression.RuleExpressionParser;

/**
 * ANTLR4 parse tree listener that evaluate logical and binary
 * expressions accordingly to grammar rules.
 *
 * @author Kai Niemi
 */
public class RuleExpressionParseTreeListener extends RuleExpressionBaseListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Parser parser;

    private final Registry registry;

    private final Deque<Object> stack = new ArrayDeque<>();

    private final Deque<Object> outcomeStack = new ArrayDeque<>();

    public RuleExpressionParseTreeListener(Parser parser, Registry registry) {
        this.parser = parser;
        this.registry = registry;
    }

    public Object pop() {
        if (!outcomeStack.isEmpty()) {
            Boolean top = popInternal(Boolean.class);
            return top ? outcomeStack.getLast() : outcomeStack.getFirst();
        }
        return popInternal(Object.class);
    }

    private <T> T popInternal(Class<T> type) {
        Object top = this.stack.pop();
        try {
            logger.trace("pop: {} [{}] after: {}", top, top.getClass().getSimpleName(), stack);
            return type.cast(top);
        } catch (ClassCastException e) {
            throw ExpressionException.create(parser, "Cannot cast '" + top + "' of type "
                    + top.getClass().getSimpleName() + " into " + type.getSimpleName());
        }
    }

    private void push(Object o) {
        if (o instanceof Number && !(o instanceof BigDecimal)) {
            stack.push(new BigDecimal(o.toString()));
        } else {
            stack.push(o);
        }
        logger.trace("push: {} [{}] after: {}", o, o.getClass().getSimpleName(), stack);
    }

    private <T> void compareAndPush(String op, Comparable<T> left, T right) {
        if (">".equals(op)) {
            push(left.compareTo(right) > 0);
        } else if (">=".equals(op)) {
            push(left.compareTo(right) >= 0);
        } else if ("<".equals(op)) {
            push(left.compareTo(right) < 0);
        } else if ("<=".equals(op)) {
            push(left.compareTo(right) <= 0);
        } else if ("==".equals(op)) {
            push(left.compareTo(right) == 0);
        } else if ("!=".equals(op)) {
            push(left.compareTo(right) != 0);
        } else {
            throw ExpressionException.create(parser, "Unknown comparison operator: " + op);
        }
    }

    private String stripQuotes(String s) {
        return s.replaceAll("(^')|('$)", "");
    }

    private void logRuleContext(String prefix, ParserRuleContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}: {}", prefix, ctx.getText());
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree tree = ctx.getChild(i);
                logger.debug("\t{}", tree.getText());
            }
        }
    }

    @Override
    public void exitComparisonExpressionStringList(RuleExpressionParser.ComparisonExpressionStringListContext ctx) {
        logRuleContext("exitComparisonExpressionStringList", ctx);

        boolean match = false;
        String key = (String) stack.pollLast();
        while (!stack.isEmpty()) {
            String v = popInternal(String.class);
            if (Objects.requireNonNull(key).equals(v)) {
                match = true;
            }
        }
        push(match);
    }

    @Override
    public void exitArithmeticMultiplicationOrDivision(
            RuleExpressionParser.ArithmeticMultiplicationOrDivisionContext ctx) {
        logRuleContext("exitArithmeticMultiplicationOrDivision", ctx);

        BigDecimal right = popInternal(BigDecimal.class);
        BigDecimal left = popInternal(BigDecimal.class);

        if (ctx.operator.getText().equals("*")) {
            this.stack.push(left.multiply(right));
        } else {
            this.stack.push(left.divide(right, RoundingMode.HALF_EVEN));
        }
    }

    @Override
    public void exitStringPlus(RuleExpressionParser.StringPlusContext ctx) {
        logRuleContext("exitStringPlus", ctx);

        String right = popInternal(String.class);
        String left = popInternal(String.class);
        push(left + right);
    }

    @Override
    public void exitArithmeticPlusOrMinus(RuleExpressionParser.ArithmeticPlusOrMinusContext ctx) {
        logRuleContext("exitArithmeticPlusOrMinus", ctx);

        BigDecimal right = popInternal(BigDecimal.class);
        BigDecimal left = popInternal(BigDecimal.class);

        if (ctx.operator.getText().equals("+")) {
            this.stack.push(left.add(right));
        } else {
            this.stack.push(left.subtract(right));
        }
    }

    @Override
    public void exitArithmeticUnaryMinusOrPlus(RuleExpressionParser.ArithmeticUnaryMinusOrPlusContext ctx) {
        logRuleContext("exitArithmeticUnaryMinusOrPlus", ctx);

        BigDecimal right = popInternal(BigDecimal.class);

        if (ctx.operator.getText().equals("-")) {
            push(right.negate());
        } else {
            push(right);
        }
    }

    @Override
    public void exitArithmeticPower(RuleExpressionParser.ArithmeticPowerContext ctx) {
        logRuleContext("exitArithmeticPower", ctx);

        BigDecimal right = popInternal(BigDecimal.class);
        BigDecimal left = popInternal(BigDecimal.class);
        if (right.stripTrailingZeros().scale() > 0) {
            throw ExpressionException.create(parser, "Floating-point power exponents are not supported: " + right);
        }
        push(left.pow(right.intValue()));
    }

    @Override
    public void exitArithmeticMinOrMax(RuleExpressionParser.ArithmeticMinOrMaxContext ctx) {
        logRuleContext("exitArithmeticMinOrMax", ctx);

        BigDecimal right = popInternal(BigDecimal.class);
        BigDecimal left = popInternal(BigDecimal.class);
        if (ctx.operator.getText().equals("min")) {
            push(left.min(right));
        } else {
            push(left.max(right));
        }
    }

    @Override
    public void exitArithmeticModulus(RuleExpressionParser.ArithmeticModulusContext ctx) {
        logRuleContext("exitArithmeticModulus", ctx);

        BigDecimal right = popInternal(BigDecimal.class);
        BigDecimal left = popInternal(BigDecimal.class);

        push(left.remainder(right));
    }

    @Override
    public void exitStringLiteral(RuleExpressionParser.StringLiteralContext ctx) {
        logRuleContext("exitStringLiteral", ctx);

        String v = ctx.getText();
        if (v.length() > 0) {
            push(v.substring(1, v.length() - 1));
        } else {
            push("");
        }
    }

    @Override
    public void exitDecimalLiteral(RuleExpressionParser.DecimalLiteralContext ctx) {
        logRuleContext("exitDecimalLiteral", ctx);

        try {
            push(new BigDecimal(ctx.getText()));
        } catch (NumberFormatException e) {
            throw ExpressionException.create(parser, e.toString());
        }
    }

    @Override
    public void exitDateTimeLiteral(RuleExpressionParser.DateTimeLiteralContext ctx) {
        logRuleContext("exitDateTimeLiteral", ctx);

        try {
            String dt = stripQuotes(ctx.DateTimeLiteral().getText());
            push(LocalDateTime.parse(dt.replace(" ", "T")));
        } catch (DateTimeParseException e) {
            throw ExpressionException.create(parser, e.toString());
        }
    }

    @Override
    public void exitBooleanLiteral(RuleExpressionParser.BooleanLiteralContext ctx) {
        logRuleContext("exitBooleanLiteral", ctx);

        String b = ctx.BooleanLiteral().getText();
        push(Boolean.parseBoolean(b));
    }

    @Override
    public void exitDateLiteral(RuleExpressionParser.DateLiteralContext ctx) {
        logRuleContext("exitDateLiteral", ctx);
        try {
            String dt = stripQuotes(ctx.DateLiteral().getText());
            push(LocalDate.parse(dt));
        } catch (DateTimeParseException e) {
            throw ExpressionException.create(parser, e.toString());
        }
    }

    @Override
    public void exitTimeLiteral(RuleExpressionParser.TimeLiteralContext ctx) {
        logRuleContext("exitTimeLiteral", ctx);

        try {
            String dt = stripQuotes(ctx.TimeLiteral().getText());
            push(LocalTime.parse(dt));
        } catch (DateTimeParseException e) {
            throw ExpressionException.create(parser, e.toString());
        }
    }

    @Override
    public void exitFunction(RuleExpressionParser.FunctionContext ctx) {
        logRuleContext("exitFunction", ctx);

        String id = ctx.Identifier().getText();

        List<Object> args = new ArrayList<>();

        logger.trace("calling function: {}", id);
        ctx.functionArguments().functionArgument().forEach(expressionContext -> {
            Object top = popInternal(Object.class);
            args.add(top);
            logger.trace("\targ: {} ({})", expressionContext.getText(), top);
        });

        Collections.reverse(args);

        try {
            Function f = registry.getFunction(id);
            Object rv = f.call(args.toArray());
            push(rv);
        } catch (Exception e) {
            parser.removeParseListeners();
            throw new ExpressionException(e);
        }
    }

    @Override
    public void exitIdentifier(RuleExpressionParser.IdentifierContext ctx) {
        logRuleContext("exitIdentifier", ctx);

        String id = ctx.Identifier().getText();
        push(registry.getVariable(id));
    }

    @Override
    public void exitOutcome(RuleExpressionParser.OutcomeContext ctx) {
        logRuleContext("exitOutcome", ctx);

        outcomeStack.push(popInternal(Object.class));
    }

    @Override
    public void exitLogicalExpressionAnd(RuleExpressionParser.LogicalExpressionAndContext ctx) {
        logRuleContext("exitLogicalExpressionAnd", ctx);

        Boolean right = popInternal(Boolean.class);
        Boolean left = popInternal(Boolean.class);
        push(left && right);
    }

    @Override
    public void exitLogicalExpressionNot(RuleExpressionParser.LogicalExpressionNotContext ctx) {
        logRuleContext("exitLogicalExpressionNot", ctx);

        Boolean right = popInternal(Boolean.class);
        push(!right);
    }

    @Override
    public void exitLogicalExpressionOr(RuleExpressionParser.LogicalExpressionOrContext ctx) {
        logRuleContext("exitLogicalExpressionOr", ctx);

        Boolean right = popInternal(Boolean.class);
        Boolean left = popInternal(Boolean.class);
        push(left || right);
    }

    @Override
    public void exitComparisonExpressionOperand(RuleExpressionParser.ComparisonExpressionOperandContext ctx) {
        logRuleContext("exitComparisonExpressionOperand", ctx);

        Object right = popInternal(Object.class);
        @SuppressWarnings("unchecked")
        Comparable<Object> left = popInternal(Comparable.class);

        String op = ctx.op.getText();
        compareAndPush(op, left, right);
    }

    @Override
    public void exitComparison_operand(RuleExpressionParser.Comparison_operandContext ctx) {
        logRuleContext("exitComparison_operand", ctx);

        push(popInternal(Object.class));
    }

    @Override
    public void exitComparisonExpressionDate(RuleExpressionParser.ComparisonExpressionDateContext ctx) {
        logRuleContext("exitComparisonExpressionDate", ctx);

        LocalDate right = popInternal(LocalDate.class);
        LocalDate left = popInternal(LocalDate.class);
        compareAndPush(ctx.op.getText(), left, right);
    }

    @Override
    public void exitComparisonExpressionTime(RuleExpressionParser.ComparisonExpressionTimeContext ctx) {
        logRuleContext("exitComparisonExpressionTime", ctx);

        LocalTime right = popInternal(LocalTime.class);
        LocalTime left = popInternal(LocalTime.class);
        compareAndPush(ctx.op.getText(), left, right);
    }

    @Override
    public void exitComparisonExpressionDateTime(RuleExpressionParser.ComparisonExpressionDateTimeContext ctx) {
        logRuleContext("exitComparisonExpressionDateTime", ctx);

        LocalDateTime right = popInternal(LocalDateTime.class);
        LocalDateTime left = popInternal(LocalDateTime.class);
        compareAndPush(ctx.op.getText(), left, right);
    }
}
