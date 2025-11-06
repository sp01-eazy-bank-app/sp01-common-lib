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
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class RabbitInboundLoggerAspect implements MethodInterceptor {

    private final IOLoggerService ioLoggerService;
    private final String appName;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        Message message = null;

        for (Object argument : arguments) {
            if (argument instanceof Message m) {
                message = m;
                break;
            }
        }

        if (message != null) {
            Map<String, String> headers = new HashMap<>();
            message.getMessageProperties().getHeaders().forEach((k, v) -> headers.put(k, String.valueOf(v)));

            String traceId = (String) headers.getOrDefault(IOLoggerConstant.TRACE_ID, IOLoggerUtil.generateTraceId());
            String payload = toJsonPayload(message.getBody());
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

    private String toJsonPayload(byte[] payloadBytes) {
        String payload;
        String rawPayload = new String(payloadBytes, StandardCharsets.UTF_8);
        try {
            Object json = new ObjectMapper().readValue(rawPayload, Object.class);
            payload = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            payload = rawPayload;   // fallback to raw if not JSON
        }
        return payload;
    }
}
