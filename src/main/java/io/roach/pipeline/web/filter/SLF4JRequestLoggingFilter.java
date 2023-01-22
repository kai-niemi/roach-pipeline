package io.roach.pipeline.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Concrete implementatio of a logging HTTP filter based on SLF4J. It adds the request log message to the
 * SLF4J mapped diagnostic context (MDC) before the request is processed, removing it again after the
 * request is processed.
 *
 * @author Kai Niemi
 */
public class SLF4JRequestLoggingFilter extends AbstractLoggingFilter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean isEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    protected void logBeforeRequestMessage(HttpServletRequest request, String message) {
        logger.trace(message);
    }

    @Override
    protected void logAfterRequestMessage(HttpServletResponse response, String message) {
        logger.trace(message);
    }

    @Override
    protected void putMdc(String key, String value) {
        MDC.put(key, value);
    }

    @Override
    protected void clearMdc() {
        MDC.clear();
    }
}
