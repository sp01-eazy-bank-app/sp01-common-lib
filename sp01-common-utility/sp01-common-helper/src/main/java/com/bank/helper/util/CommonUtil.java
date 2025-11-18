package com.bank.helper.util;

import java.util.UUID;

public class CommonUtil {

    private CommonUtil() {}

    public static final String TRACE_ID = "traceId";

    public static String generateTraceId() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }

    public static String generateStandardId(String identifierPrefix) {
        return identifierPrefix + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 7)
                .toUpperCase();
    }
}
