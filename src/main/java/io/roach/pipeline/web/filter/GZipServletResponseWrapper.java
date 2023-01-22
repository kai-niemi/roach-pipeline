package io.roach.pipeline.web.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class GZipServletResponseWrapper extends HttpServletResponseWrapper {
    private PrintWriter printWriter;

    private GZipServletOutputStream gzipOutputStream;

    public GZipServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void close() throws IOException {
        if (this.printWriter != null) {
            this.printWriter.close();
        }
        if (this.gzipOutputStream != null) {
            this.gzipOutputStream.close();
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        if (this.printWriter != null) {
            this.printWriter.flush();
        }
        if (this.gzipOutputStream != null) {
            this.gzipOutputStream.flush();
        }
        super.flushBuffer();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.printWriter != null) {
            throw new IllegalStateException("Output stream already created");
        }
        if (this.gzipOutputStream == null) {
            this.gzipOutputStream = new GZipServletOutputStream(getResponse().getOutputStream());
        }
        return this.gzipOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.printWriter == null && this.gzipOutputStream != null) {
            throw new IllegalStateException("Output writer already created");
        }
        if (this.printWriter == null) {
            this.gzipOutputStream = new GZipServletOutputStream(getResponse().getOutputStream());
            this.printWriter = new PrintWriter(new OutputStreamWriter(this.gzipOutputStream,
                    getResponse().getCharacterEncoding()));
        }
        return this.printWriter;
    }

    @Override
    public void setContentLength(int len) {
    }
}