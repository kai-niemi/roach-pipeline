package io.roach.pipeline.web.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GzipRequestFilter extends OncePerRequestFilter {
    private static final Set<String> IGNORE_METHODS = new HashSet<>(Arrays.asList("GET", "OPTIONS", "HEAD"));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod().toUpperCase();
        String encoding = request.getHeader(HttpHeaders.CONTENT_ENCODING);

        if (!IGNORE_METHODS.contains(method)
                && StringUtils.hasLength(encoding) && encoding.contains("application/gzip")) {
            filterChain.doFilter(new GzippedInputStreamWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
