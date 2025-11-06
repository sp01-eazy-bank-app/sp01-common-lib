package com.bank.iolog.aspect;

import com.bank.iolog.service.IOLoggerService;
import com.bank.iolog.util.IOLoggerConstant;
import com.bank.iolog.util.IOLoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class IOLoggerAspect {

    private final IOLoggerService ioLoggerService;

    @Value("${spring.application.name:unknown-service}")
    private String sourceApplication;

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logInboundAndOutboundCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return joinPoint.proceed();

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        if (!(request instanceof ContentCachingRequestWrapper wrappedRequest)
                || !(response instanceof ContentCachingResponseWrapper wrappedResponse)) {
            return joinPoint.proceed();
        }

        String traceId = Optional.ofNullable(request.getHeader(IOLoggerConstant.TRACE_ID))
                .filter(s -> !s.isEmpty())
                .orElse(IOLoggerUtil.generateTraceId());
        wrappedRequest.setAttribute(IOLoggerConstant.TRACE_ID, traceId);

        String resource = IOLoggerUtil.buildFullResource(request);

        ioLoggerService.logHttpInboundRequest(wrappedRequest, traceId, sourceApplication, resource);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            log.error("Controller threw exception for traceId={}", traceId, e);
            throw e;
        } finally {
            wrappedResponse.copyBodyToResponse();
        }

        Map<String, String> responseHeaders = wrappedResponse.getHeaderNames().stream()
                .collect(Collectors.toMap(h -> h, wrappedResponse::getHeader));

        ioLoggerService.logHttpOutboundResponse(
                wrappedResponse, traceId, sourceApplication, resource,
                wrappedResponse.getStatus(), responseHeaders
        );

        return result;
    }
}
