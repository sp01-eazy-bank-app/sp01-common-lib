package com.bank.iolog.aspect;

import com.bank.iolog.service.IOLoggerService;
import com.bank.iolog.util.IOLoggerConstant;
import com.bank.iolog.util.IOLoggerUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Aspect
@RequiredArgsConstructor
public class RabbitOutboundLoggerAspect {

    private final IOLoggerService ioLoggerService;

    @Value("${spring.application.name}")
    private String appName;

    // --- OUTBOUND (Producer) ---
    @Around("execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.convertAndSend(..))")
    public Object logRabbitOutboundRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String exchange = args.length > 0 ? "Exchange: " + args[0] : "UNKNOWN_EXCHANGE";
        String routingKey = args.length > 1 ? "RoutingKey: " + args[1] : "UNKNOWN_ROUTING_KEY";
        Object payload = args.length > 2 ? args[2] : null;

        Map<String, String> headers = new HashMap<>();
        if (payload instanceof Message message) {
            message.getMessageProperties().getHeaders()
                    .forEach((k, v) -> headers.put(k, String.valueOf(v)));
        }

        String traceId = Optional.ofNullable(MDC.get(IOLoggerConstant.TRACE_ID))
                .orElseGet(IOLoggerUtil::generateTraceId);
        headers.putIfAbsent(IOLoggerConstant.TRACE_ID, traceId);

        String resource = exchange + " : " + routingKey;
        ioLoggerService.logRabbitOutboundResponse(payload, traceId, appName, resource, 204, headers);

        return joinPoint.proceed();
    }
}
