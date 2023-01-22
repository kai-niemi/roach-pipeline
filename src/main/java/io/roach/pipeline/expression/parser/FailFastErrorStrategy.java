package io.roach.pipeline.expression.parser;

import java.util.BitSet;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import io.roach.pipeline.expression.ExpressionException;

/**
 * ANTLR4 error strategy that propagates all parse errors without
 * any recovery attempts.
 *
 * @author Kai Niemi
 */
public class FailFastErrorStrategy extends DefaultErrorStrategy implements ANTLRErrorListener {
    /**
     * Make sure we don't attempt to recover from problems in subrules.
     */
    @Override
    public void sync(Parser recognizer) {
    }

    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        for (ParserRuleContext context = recognizer.getContext(); context != null; context = context.getParent()) {
            context.exception = e;
        }
        throw ExpressionException.create(recognizer, e.toString());
    }

    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
        InputMismatchException e = new InputMismatchException(recognizer);
        for (ParserRuleContext context = recognizer.getContext(); context != null; context = context.getParent()) {
            context.exception = e;
        }

        String msg = "Mismatched input " + getTokenErrorDisplay(e.getOffendingToken())
                + ". Expecting one of: " + e.getExpectedTokens().toString(recognizer.getVocabulary());

        throw ExpressionException.create(recognizer, msg);
    }

    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        if (!inErrorRecoveryMode(recognizer)) {
            if (e instanceof NoViableAltException) {
                reportNoViableAlternative(recognizer, (NoViableAltException) e);
            } else if (e instanceof InputMismatchException) {
                reportInputMismatch(recognizer, (InputMismatchException) e);
            } else if (e instanceof FailedPredicateException) {
                reportFailedPredicate(recognizer, (FailedPredicateException) e);
            } else {
                recognizer.removeParseListeners();
                recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
            }
        }
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException cause) {
        String msg = "No viable alternative input for "
                + getTokenErrorDisplay(cause.getOffendingToken())
                + ". Expecting one of: " + cause.getExpectedTokens().toString(recognizer.getVocabulary());
        throw ExpressionException.create(recognizer, msg);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException cause) {
        String msg = "Mismatched input " + getTokenErrorDisplay(cause.getOffendingToken())
                + ". Expecting one of: " + cause.getExpectedTokens().toString(recognizer.getVocabulary());
        throw ExpressionException.create(recognizer, msg);
    }

    @Override
    public void reportMissingToken(Parser recognizer) {
        String msg = "Missing " + getExpectedTokens(recognizer).toString(recognizer.getVocabulary())
                + " at " + getTokenErrorDisplay(recognizer.getCurrentToken());
        throw ExpressionException.create(recognizer, msg);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        String msg = "Unwanted token " + getTokenErrorDisplay(recognizer.getCurrentToken())
                + " expected " + getExpectedTokens(recognizer).toString(recognizer.getVocabulary());
        throw ExpressionException.create(recognizer, msg);
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                            int charPositionInLine,
                            String msg, RecognitionException e) {
        throw new ExpressionException(msg + " near pos " + charPositionInLine);
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
                                BitSet ambigAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                            BitSet conflictingAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                         int prediction,
                                         ATNConfigSet configs) {
    }
}
