package com.bank.iolog.filter;

import com.bank.iolog.service.IOLoggerService;
import com.bank.iolog.util.IOLoggerConstant;
import com.bank.iolog.util.IOLoggerUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestWrappingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestWrappingFilter.class);

    private final IOLoggerService ioLoggerService;
    private final String sourceApplication;

    public RequestWrappingFilter(IOLoggerService ioLoggerService, String sourceApplication) {
        this.ioLoggerService = ioLoggerService;
        this.sourceApplication = sourceApplication;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Prepare wrappers (prefer CachedBodyHttpServletRequest when possible)
        ContentCachingRequestWrapper wrappedRequest;
        CachedBodyHttpServletRequest cachedRequest = null;

        if (request instanceof ContentCachingRequestWrapper) {
            wrappedRequest = (ContentCachingRequestWrapper) request;
        } else {
            try {
                cachedRequest = new CachedBodyHttpServletRequest(request);
                wrappedRequest = new ContentCachingRequestWrapper(cachedRequest);
            } catch (IOException e) {
                wrappedRequest = new ContentCachingRequestWrapper(request);
            }
        }

        ContentCachingResponseWrapper wrappedResponse = response instanceof ContentCachingResponseWrapper
                ? (ContentCachingResponseWrapper) response
                : new ContentCachingResponseWrapper(response);

        // record start time for ordering
        Instant startTs = Instant.now();
        wrappedRequest.setAttribute(IOLoggerConstant.REQUEST_START_TIME, startTs);

        // make wrapped request/response visible through RequestContextHolder
        ServletRequestAttributes previousAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(wrappedRequest, wrappedResponse));

        try {
            // Proceed with filter chain
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // Resolve request body, trace id, resource and response headers using helper methods
            String cachedBody = resolveRequestBody(wrappedRequest, cachedRequest);
            String traceId = resolveTraceId(wrappedRequest);
            String resource = IOLoggerUtil.buildFullResource(wrappedRequest);
            Map<String, String> responseHeaders = collectResponseHeaders(wrappedResponse);

            // Debug lengths (best-effort)
            try {
                int reqLen = cachedBody != null ? cachedBody.length() : wrappedRequest.getContentAsByteArray().length;
                int respLen = wrappedResponse.getContentAsByteArray().length;
                log.debug("IOLogger: traceId={} resource={} reqLen={} respLen={}", traceId, resource, reqLen, respLen);
            } catch (Exception e) {
                log.debug("IOLogger: unable to compute body lengths", e);
            }

            // Log inbound and outbound using helper methods (best-effort)
            logInbound(cachedBody, wrappedRequest, traceId, resource, startTs);
            logOutbound(wrappedResponse, traceId, resource, responseHeaders);

        } finally {
            // Ensure response body is copied back and restore previous RequestAttributes
            try {
                wrappedResponse.copyBodyToResponse();
            } finally {
                if (previousAttributes != null) {
                    RequestContextHolder.setRequestAttributes(previousAttributes);
                } else {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        }
    }

    // Helper: resolve the request body from attribute, cached wrapper, or content wrapper
    private String resolveRequestBody(ContentCachingRequestWrapper wrappedRequest, CachedBodyHttpServletRequest cachedRequest) {
        try {
            Object attr = wrappedRequest.getAttribute(IOLoggerConstant.REQUEST_BODY);
            if (attr instanceof String s && !s.isEmpty()) return s;

            if (cachedRequest != null) {
                byte[] b = cachedRequest.getCachedBody();
                if (b.length > 0) {
                    String enc = wrappedRequest.getCharacterEncoding() != null ? wrappedRequest.getCharacterEncoding() : StandardCharsets.UTF_8.name();
                    String body = new String(b, enc);
                    wrappedRequest.setAttribute(IOLoggerConstant.REQUEST_BODY, body);
                    return body;
                }
            }

            byte[] content = wrappedRequest.getContentAsByteArray();
            if (content.length > 0) {
                String encoding = wrappedRequest.getCharacterEncoding() != null ? wrappedRequest.getCharacterEncoding() : StandardCharsets.UTF_8.name();
                String body = new String(content, encoding);
                wrappedRequest.setAttribute(IOLoggerConstant.REQUEST_BODY, body);
                return body;
            }
        } catch (Exception e) {
            log.debug("IOLogger: failed to resolve request body", e);
        }
        return null;
    }

    // Helper: build or reuse a trace id and store it on the request
    private String resolveTraceId(ContentCachingRequestWrapper wrappedRequest) {
        String traceId = Optional.ofNullable(wrappedRequest.getHeader(IOLoggerConstant.TRACE_ID))
                .filter(s -> !s.isEmpty())
                .orElse(null);
        if (traceId == null) {
            Object attrObj = wrappedRequest.getAttribute(IOLoggerConstant.TRACE_ID);
            if (attrObj != null) traceId = attrObj.toString();
        }
        if (traceId == null) traceId = IOLoggerUtil.generateTraceId();
        wrappedRequest.setAttribute(IOLoggerConstant.TRACE_ID, traceId);
        return traceId;
    }

    // Helper: collect response headers into a simple map
    private Map<String, String> collectResponseHeaders(ContentCachingResponseWrapper wrappedResponse) {
        return wrappedResponse.getHeaderNames().stream()
                .collect(Collectors.toMap(h -> h, h -> Optional.ofNullable(wrappedResponse.getHeader(h)).orElse("")));
    }

    // Extracted logging helpers for better readability
    private void logInbound(String cachedBody, ContentCachingRequestWrapper wrappedRequest, String traceId, String resource, Instant startTs) {
        try {
            if (cachedBody != null) {
                ioLoggerService.logHttpInboundWithPayload(cachedBody, wrappedRequest, traceId, sourceApplication, resource, startTs);
            } else {
                ioLoggerService.logHttpInboundRequest(wrappedRequest, traceId, sourceApplication, resource);
            }
        } catch (Exception e) {
            log.debug("IOLogger: inbound logging failed for traceId={}", traceId, e);
        }
    }

    private void logOutbound(ContentCachingResponseWrapper wrappedResponse, String traceId, String resource, Map<String, String> responseHeaders) {
        try {
            ioLoggerService.logHttpOutboundResponse(wrappedResponse, traceId, sourceApplication, resource,
                    wrappedResponse.getStatus(), responseHeaders);
        } catch (Exception e) {
            log.debug("IOLogger: outbound logging failed for traceId={}", traceId, e);
        }
    }
}
