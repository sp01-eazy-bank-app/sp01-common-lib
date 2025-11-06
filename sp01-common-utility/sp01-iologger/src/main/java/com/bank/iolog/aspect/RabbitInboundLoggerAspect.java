package com.bank.iolog.aspect;

import com.bank.iolog.service.IOLoggerService;
import com.bank.iolog.util.IOLoggerConstant;
import com.bank.iolog.util.IOLoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RabbitInboundLoggerAspect implements MethodInterceptor {

    private final IOLoggerService ioLoggerService;
    private final String appName;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = Arrays.stream(invocation.getArguments())
                .filter(Message.class::isInstance)
                .map(Message.class::cast)
                .findFirst()
                .orElse(null);

        if (message != null) {
            Map<String, String> headers = message.getMessageProperties().getHeaders().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

            String traceId = headers.getOrDefault(IOLoggerConstant.TRACE_ID, IOLoggerUtil.generateTraceId());
            String payload = parseJsonSafely(message.getBody());
            String queueName = "Queue: " + message.getMessageProperties().getConsumerQueue();

            ioLoggerService.logRabbitInboundRequest(headers, payload, traceId, appName, queueName);
            MDC.put(IOLoggerConstant.TRACE_ID, traceId);
        }

        try {
            return invocation.proceed();
        } finally {
            MDC.remove(IOLoggerConstant.TRACE_ID);
        }
    }

    private String parseJsonSafely(byte[] payloadBytes) {
        String raw = new String(payloadBytes, StandardCharsets.UTF_8);
        try {
            Object json = MAPPER.readValue(raw, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return raw;
        }
    }
}
