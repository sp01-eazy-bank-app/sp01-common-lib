package com.bank.iolog.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public class IOLoggerUtil {

    private IOLoggerUtil() {}

    public static String generateTraceId() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }

    public static String buildFullResource(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return request.getMethod() + " " + (query == null ? uri : uri + "?" + query);
    }
}
