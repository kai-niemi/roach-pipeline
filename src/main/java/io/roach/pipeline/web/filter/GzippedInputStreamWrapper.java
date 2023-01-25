package io.roach.pipeline.web.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class GzippedInputStreamWrapper extends HttpServletRequestWrapper {
    private final GZIPInputStream inputStream;

    GzippedInputStreamWrapper(final HttpServletRequest request) throws IOException {
        super(request);
        this.inputStream = new GZIPInputStream(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() {
        return new DelegatingServletInputStream(this.inputStream);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.inputStream));
    }
}

