package com.bank.iolog.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Simple request wrapper that reads the entire body into memory on construction
 * and returns a repeatable input stream/reader for downstream handlers.
 * Keep small and focused to avoid external deps.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final String characterEncoding;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.characterEncoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : StandardCharsets.UTF_8.name();
        // Read the input stream into a byte array
        byte[] body = new byte[0];
        try {
            body = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            // leave empty
        }
        this.cachedBody = body != null ? body : new byte[0];
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(this.cachedBody);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        Charset cs = Charset.forName(this.characterEncoding);
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(this.cachedBody), cs));
    }

    public byte[] getCachedBody() {
        return this.cachedBody;
    }
}

