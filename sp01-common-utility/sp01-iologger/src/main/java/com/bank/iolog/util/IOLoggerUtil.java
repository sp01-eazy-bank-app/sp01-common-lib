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
        StringBuffer url = request.getRequestURL();
        String query = request.getQueryString();
        String full = (query == null || query.isEmpty()) ? url.toString() : url.append('?').append(query).toString();
        return request.getMethod() + " " + full;
    }
}
