package com.bank.iolog.aspect;

import com.bank.iolog.service.IOLoggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class IOLoggerAspect {

    private final IOLoggerService ioLoggerService;

    // NOTE: REST controller logging is handled by the RequestWrappingFilter to ensure payloads are cached.
    // Keep this aspect for non-HTTP usages if necessary (e.g., service-layer cross-cutting). For now it's a no-op for controllers.
    @Around("within(@org.springframework.stereotype.Controller *) || within(@org.springframework.stereotype.Service *)")
    public Object noop(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
