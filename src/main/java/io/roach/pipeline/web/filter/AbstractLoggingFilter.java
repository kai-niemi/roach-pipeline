package io.roach.pipeline.web.filter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Verbose servlet request/response logging filter. It logs the client request details as well as
 * the server response back the client. The request headers, request entity, response headers and
 * response entity can be logged on demand. By default, only the request line and response line
 * are logged.
 *
 * @author Kai Niemi
 */
public abstract class AbstractLoggingFilter extends OncePerRequestFilter {
    private static final ThreadLocal<RequestId> REQUEST_ID_THREAD_LOCAL = new ThreadLocal<>();

    private static final AtomicLong COUNTER = new AtomicLong();

    /**
     * MDC key for request method.
     */
    public static final String HTTP_METHOD = "http.method";

    /**
     * MDC key for request URI.
     */
    public static final String HTTP_URI = "http.uri";

    /**
     * MDC key for request client IP.
     */
    public static final String HTTP_CLIENT = "http.client";

    /**
     * MDC key for request client session.
     */
    public static final String HTTP_SESSION = "http.session";

    /**
     * MDC key for request user token.
     */
    public static final String HTTP_USER = "http.user";

    /**
     * MDC key for request line.
     */
    public static final String HTTP_REQ = "http.req";

    private static final class RequestId {
        final long id = COUNTER.incrementAndGet();

        final long requestTime = System.currentTimeMillis();
    }

    private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 1024;

    private static final char NL = '\n';

    private static final String NOTIFICATION_PREFIX = "* ";

    private static final String REQUEST_PREFIX = "> ";

    private static final String RESPONSE_PREFIX = "< ";

    private boolean includeQueryString;

    private boolean includeClientInfo = true;

    private boolean includePayload;

    private boolean includeHeaders;

    private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

    private boolean enabled = true;

    private final AtomicInteger requestCount = new AtomicInteger();

    private final AtomicLong requestPayloadBytes = new AtomicLong();

    private final AtomicLong responsePayloadBytes = new AtomicLong();

    public int getRequestCount() {
        return requestCount.get();
    }

    public long getRequestPayloadBytes() {
        return requestPayloadBytes.get();
    }

    public long getResponsePayloadBytes() {
        return responsePayloadBytes.get();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeQueryString() {
        return includeQueryString;
    }

    public void setIncludeQueryString(boolean includeQueryString) {
        this.includeQueryString = includeQueryString;
    }

    public boolean isIncludeClientInfo() {
        return includeClientInfo;
    }

    public void setIncludeClientInfo(boolean includeClientInfo) {
        this.includeClientInfo = includeClientInfo;
    }

    public boolean isIncludePayload() {
        return includePayload;
    }

    public void setIncludePayload(boolean includePayload) {
        this.includePayload = includePayload;
    }

    public boolean isIncludeHeaders() {
        return includeHeaders;
    }

    public void setIncludeHeaders(boolean includeHeaders) {
        this.includeHeaders = includeHeaders;
    }

    public int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    public void setMaxPayloadLength(int maxPayloadLength) {
        if (maxPayloadLength <= 0) {
            throw new IllegalArgumentException("Max payload length must be > 0");
        }
        this.maxPayloadLength = maxPayloadLength;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        requestCount.incrementAndGet();

        // Read something in the original request before wrapping
        request.getParameterNames();

        if (isIncludePayload()) {
            request = new ContentCachingRequestWrapper(request);
            response = new ContentCachingResponseWrapper(response);
        }

        logBeforeRequest(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            logAfterRequest(response);
        }
    }

    protected void logBeforeRequest(HttpServletRequest request) throws IOException {
        setId();
        StringBuilder builder = new StringBuilder();
        printRequestLine(builder, request);
        if (isIncludeHeaders()) {
            printRequestHeaders(builder, request);
        }
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper contentCachingRequestWrapper = (ContentCachingRequestWrapper) request;
            requestPayloadBytes.addAndGet(contentCachingRequestWrapper.getContentLength());
            printEntity(builder, new String(contentCachingRequestWrapper.getContentAsByteArray(),
                    contentCachingRequestWrapper.getCharacterEncoding()));
        }
        logBeforeRequestMessage(request, builder.toString());
    }

    /**
     * Concrete subclasses should implement this method to write a log message <i>before</i> the request is processed.
     *
     * @param request current HTTP request
     * @param message the message to log
     * @throws IOException on I/O errors
     */
    protected abstract void logBeforeRequestMessage(HttpServletRequest request, String message) throws IOException;

    /**
     * Concrete subclasses should implement this method to write a log message <i>after</i> the request is processed.
     *
     * @param response current HTTP response
     * @param message the message to log
     * @throws IOException on I/O errors
     */
    protected abstract void logAfterRequestMessage(HttpServletResponse response, String message) throws IOException;

    /**
     * Register a request MDC key and value (typically used in a logging system).
     *
     * @param key the key
     * @param value the value
     */
    protected abstract void putMdc(String key, String value);

    /**
     * Clear the current request MDC.
     */
    protected abstract void clearMdc();

    private void setId() {
        REQUEST_ID_THREAD_LOCAL.set(new RequestId());
    }

    private StringBuilder prefixId(StringBuilder b) {
        long counter = REQUEST_ID_THREAD_LOCAL.get().id;
        b.append(NL)
                .append(counter).append(" ");
        return b;
    }

    private void printRequestLine(StringBuilder builder, HttpServletRequest request) {
        StringBuilder line = new StringBuilder();
        line.append(request.getMethod()).append(" ").append(request.getRequestURI());

        if (isIncludeQueryString() && request.getQueryString() != null) {
            line.append('?').append(request.getQueryString());
        }

        putMdc(HTTP_METHOD, request.getMethod());
        putMdc(HTTP_URI, request.getRequestURI());
        String client = request.getRemoteAddr();
        if (StringUtils.hasLength(client)) {
            if (isIncludeClientInfo()) {
                line.append(";client=").append(client);
            }
            putMdc(HTTP_CLIENT, client);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            if (isIncludeClientInfo()) {
                line.append(";session=").append(session.getId());
            }
            putMdc(HTTP_SESSION, session.getId());
        }
        String user = request.getRemoteUser();
        if (user != null) {
            if (isIncludeClientInfo()) {
                line.append(";user=").append(user);
            }
            putMdc(HTTP_USER, user);
        }

        putMdc(HTTP_REQ, line.toString());

        prefixId(builder).append(NOTIFICATION_PREFIX).append("Server in-bound request");
        prefixId(builder).append(REQUEST_PREFIX).append(line);
    }

    private void printRequestHeaders(StringBuilder b, HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = (String) headerValues.nextElement();
                prefixId(b).append(REQUEST_PREFIX)
                        .append(headerName).append(": ").append(headerValue);
            }
        }
        prefixId(b).append(REQUEST_PREFIX);
    }

    private void printEntity(StringBuilder b, String body) {
        if (body.length() > 0) {
            int length = Math.min(body.length(), getMaxPayloadLength());
            b.append(body, 0, length);
        }
    }

    protected void logAfterRequest(HttpServletResponse response) throws IOException {
        StringBuilder builder = new StringBuilder();

        printResponseLine(builder, response);
        if (isIncludeHeaders()) {
            printResponseHeaders(builder, response);
        }
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper contentCachingResponseWrapper = (ContentCachingResponseWrapper) response;
            responsePayloadBytes.addAndGet(contentCachingResponseWrapper.getContentSize());
            printEntity(builder, new String(contentCachingResponseWrapper.getContentAsByteArray(),
                    contentCachingResponseWrapper.getCharacterEncoding()));
            contentCachingResponseWrapper.copyBodyToResponse();
        }
        logAfterRequestMessage(response, builder.toString());
        clearMdc();
    }

    private void printResponseLine(StringBuilder b, HttpServletResponse response) {
        prefixId(b)
                .append(NOTIFICATION_PREFIX)
                .append("Server out-bound response");

        long requestTime = REQUEST_ID_THREAD_LOCAL.get().requestTime;
        long diff = System.currentTimeMillis() - requestTime;
        prefixId(b)
                .append(RESPONSE_PREFIX)
                .append("# Round-trip time (ms): ")
                .append(diff);

        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            prefixId(b)
                    .append(RESPONSE_PREFIX)
                    .append("# Status code: ")
                    .append(wrapper.getStatus());
        }
    }

    private void printResponseHeaders(StringBuilder b, HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            if (!wrapper.getHeaderNames().isEmpty()) {
                for (String headerName : wrapper.getHeaderNames()) {
                    for (Object headerValue : wrapper.getHeaders(headerName)) {
                        prefixId(b).append(RESPONSE_PREFIX)
                                .append(headerName).append(": ").append(headerValue);
                    }
                }
                prefixId(b).append(RESPONSE_PREFIX);
            }
        }
    }
}
