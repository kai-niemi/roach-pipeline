package io.roach.pipeline.web.filter;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GzipResponseFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (acceptsGZipEncoding(request)) {
            response.addHeader("Content-Encoding", "gzip");
            GZipServletResponseWrapper responseWrapper = new GZipServletResponseWrapper(response);
            filterChain.doFilter(request, responseWrapper);
            responseWrapper.close();
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean acceptsGZipEncoding(HttpServletRequest httpRequest) {
        String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
        return acceptEncoding != null && acceptEncoding.contains("gzip");
    }
}
